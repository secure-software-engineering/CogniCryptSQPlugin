
# Complete SonarQube SECAI Plugin Server Setup & Deployment Guide

This comprehensive guide covers both local development setup and production server deployment for the SonarQube SECAI plugin stack.

### Overview

The SECAI plugin extends SonarQube with AI-powered code analysis capabilities, integrating OpenAI and Google APIs for enhanced security analysis. This setup includes three main components:

- **SonarQube** with the SECAI plugin
- **PostgreSQL** database for SonarQube data
- **Flask application** for additional AI processing


## Production Server Deployment

### Prerequisites

Before beginning any deployment, ensure you have:

- A server instance (Ubuntu 22.04 LTS recommended for production)
- SSH access with sudo privileges (for production)
- Docker and Docker Compose installed ([Installation Guide](https://docs.docker.com/get-docker/))
- An OpenAI API key
- A Google API key
- Java 17+ and Maven (for local development)
- Git installed for repository cloning
- Minimum 4GB RAM and 20GB disk space
- Network access to download dependencies

For production environments, use this comprehensive setup that includes all necessary components and security configurations.

### Server Preparation

1. **Update System and Install Docker**
   ```bash
   # Update package list
   sudo apt-get update
   
   # Install HTTPS repository dependencies
   sudo apt-get install -y \
       apt-transport-https \
       ca-certificates \
       curl \
       gnupg \
       lsb-release
   ```

2. **Add Docker Repository**
   ```bash
   # Add Docker's GPG key
   sudo mkdir -p /etc/apt/keyrings
   curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
   
   # Set up repository
   echo \
     "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
     $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
   ```

3. **Install Docker**
   ```bash
   sudo apt-get update
   sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
   
   # Verify installation
   sudo docker --version
   sudo docker compose version
   ```
   
   > **Note**: For detailed installation instructions for other operating systems, visit the [official Docker installation guide](https://docs.docker.com/get-docker/).

### Production Stack Setup

1. **Create Project Structure**
   ```bash
   mkdir sonarqube_secai
   cd sonarqube_secai
   ```

2. **Create Production "Dockerfile"**
   ```dockerfile
   # Install JDK + tools
   ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
   RUN apt-get update \
   && apt-get install -y --no-install-recommends openjdk-21-jdk-headless curl unzip git ca-certificates \
   && rm -rf /var/lib/apt/lists/*


   # Install Maven
   ENV MAVEN_VERSION=3.9.11
   ENV MAVEN_HOME=/opt/maven
   RUN apt-get update && \
      apt-get install -y curl unzip git && \
      curl -fsSL https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-
   bin.tar.gz | \
      tar -xz -C /opt && \
      ln -s /opt/apache-maven-${MAVEN_VERSION} ${MAVEN_HOME} && \
      ln -s ${MAVEN_HOME}/bin/mvn /usr/bin/mvn

   # Install Gradle
   ENV GRADLE_VERSION=8.5
   ENV GRADLE_HOME=/opt/gradle
   RUN curl -fsSL https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -o gradle.zip && \
      unzip gradle.zip -d /opt && \
      rm gradle.zip && \
      ln -s /opt/gradle-${GRADLE_VERSION} ${GRADLE_HOME} && \
      ln -s ${GRADLE_HOME}/bin/gradle /usr/bin/gradle

   USER sonarqube

   ENV PATH="${JAVA_HOME}/bin:${MAVEN_HOME}/bin:${GRADLE_HOME}/bin:${PATH}"
   ENV SONAR_JAVA_PATH = "/usr/lib/jvm/java-21-openjdk-amd64/bin/java"
   ```

3. **Create Production Docker Compose (docker-compose.yml)**
   ```yaml
   services:
      sonarqube:
         build: .
         container_name: sonarqube
         ports:
            - "9000:9000"
         environment:
            - MAVEN_HOME=/opt/maven
            - GRADLE_HOME=/opt/gradle
            - HOST_USER_HOME=/root
            - SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true
            - OPEN_AI_API_KEY=${OPEN_AI_API_KEY}
            - SONAR_JAVA_PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin/java
         volumes:
            - sonarqube_data:/opt/sonarqube/data
            - sonarqube_extensions:/opt/sonarqube/extensions
            - ./plugins:/opt/sonarqube/extensions/plugins
            - ./projects:/projects
            - /root/secai:/home/sonarqube/secai
         networks:
            -  scoobydoo
         restart: always

      flaskapp:
         build: ./flaskapp
         container_name: flaskapp
         ports:
            - "8000:8000"
         expose:
            - "8000"
         env_file:
            - ./flaskapp/.env
         networks:
            -  scoobydoo
         volumes:
            - ./flaskapp:/app
         restart: always

      confidence:
         build: ./confidence
         container_name: confidence
         ports:
            - "80:8001"
         expose:
            - "8001"
         networks:
            - scoobydoo
         volumes:
            - ./confidence:/app
         restart: always

      volumes:
         sonarqube_data:
         sonarqube_extensions:

      networks:
         scoobydoo:
            driver: bridge
   ```

### Component Installation

1. **Set Up Flask Application**
   
   This is the official Flask application that powers the AI Fix functionality for code generation and security analysis. The application integrates with OpenAI and Google APIs to provide intelligent code fixes and suggestions.
   
   ```bash
   git clone https://github.com/secure-software-engineering/pg-secai.git flaskapp
   ```
   
   > **Repository**: [https://github.com/secure-software-engineering/pg-secai](https://github.com/secure-software-engineering/pg-secai)

   **Create flaskapp "Dockerfile"**   
   ```dockerfile
      # Use official Python image
      FROM python:3.10-slim

      # Install JDK + tools
      ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
      RUN apt-get update \
      && apt-get install -y --no-install-recommends openjdk-21-jdk-headless curl unzip git ca-certificates \
      && rm -rf /var/lib/apt/lists/*

      # Set JAVA_HOME and PATH (works on Debian-based python:slim)
      ENV JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
      ENV PATH="$JAVA_HOME/bin:${PATH}"

      # Set working directory
      WORKDIR /app

      # Copy requirements and install dependencies
      COPY requirements.txt .
      RUN pip install --no-cache-dir -r requirements.txt

      # Copy rest of the codebase
      COPY . .

      # Expose port for Flask
      EXPOSE 5000

      # Run the app
      CMD ["python", "main.py"]
   ```

2. **Download SECAI Plugin**
   
   This is the official SECAI SonarQube plugin that extends SonarQube with AI-powered security analysis capabilities. Once installed, it appears in the SonarQube interface and provides enhanced code analysis, error tree visualization, and AI-driven code fixes.
   
   ```bash
   mkdir plugin
   wget -P ./plugin https://github.com/secure-software-engineering/SonarQubePlugin/releases/download/v1.0/secai-sonarqube-plugin-1.0.jar
   ```
   
   > **Repository**: [https://github.com/secure-software-engineering/SonarQubePlugin](https://github.com/secure-software-engineering/SonarQubePlugin)

3. **Set Up Confidence Analysis**
   ```bash
   mkdir confidence
   wget -P ./confidence https://raw.githubusercontent.com/secure-software-engineering/SonarQubePlugin/main/confidence-main/main.py
   ```

   **Create confidence score "Dockerfile"**
   ```dockerfile
      # Use official Python image
      FROM python:3.11-slim

      # Set working directory
      WORKDIR /app

      # Copy requirements and install dependencies
      COPY requirements.txt .
      RUN pip install --no-cache-dir -r requirements.txt

      # Copy rest of the codebase
      COPY . .

      # Expose port for Flask
      EXPOSE 8001

      # Run the app
      CMD ["python", "main.py"]
   ```

### Environment Configuration

Environment files store sensitive configuration data like API keys. Create these files to configure the services properly.

1. **Root Environment File (.env)**
   
   Create the main environment file in the project root directory:
   
   ```bash
   # Create the root .env file using cat command
   cat > .env << 'EOF'
   # OpenAI API Configuration
   OPEN_AI_API_KEY=your_openai_api_key_here
   
   # Optional: Additional SonarQube configurations
   # SONAR_JDBC_URL=jdbc:postgresql://db:5432/sonar
   # SONAR_JDBC_USERNAME=sonar
   # SONAR_JDBC_PASSWORD=sonar
   EOF
   ```
   
   **Alternative method using echo:**
   ```bash
   echo "OPEN_AI_API_KEY=your_openai_api_key_here" > .env
   ```
   
   **How to get OpenAI API Key:**
   - Visit [https://platform.openai.com/api-keys](https://platform.openai.com/api-keys)
   - Sign in to your OpenAI account
   - Click "Create new secret key"
   - Copy the generated key and replace `your_openai_api_key_here`

2. **Flask Application Environment (flaskapp/.env)**
   
   Create the Flask application environment file:
   
   ```bash
   # Create the Flask app .env file using cat command
   cat > flaskapp/.env << 'EOF'
   # OpenAI API Configuration
   OPEN_AI_API_KEY=your_openai_api_key_here
   
   # Google API Configuration
   GOOGLE_API_KEY=your_google_api_key_here
   
   # Flask Configuration
   FLASK_ENV=production
   FLASK_DEBUG=false
   EOF
   ```
   
   **Alternative method using multiple echo commands:**
   ```bash
   echo "OPEN_AI_API_KEY=your_openai_api_key_here" > flaskapp/.env
   echo "GOOGLE_API_KEY=your_google_api_key_here" >> flaskapp/.env
   echo "FLASK_ENV=production" >> flaskapp/.env
   echo "FLASK_DEBUG=false" >> flaskapp/.env
   ```
   
   **How to get Google API Key:**
   - Visit [Google Cloud Console](https://console.cloud.google.com/)
   - Create a new project or select existing one
   - Enable required APIs (e.g., Google AI Platform)
   - Go to "Credentials" → "Create Credentials" → "API Key"
   - Copy the generated key and replace `your_google_api_key_here`

   > **Security Note**: Never commit `.env` files to version control. Add them to your `.gitignore` file.

### Deployment

1. **Build and Start Services**
   ```bash
   sudo docker compose build
   sudo docker compose up -d
   ```

2. **Monitor Startup**
   ```bash
   sudo docker compose logs -f sonarqube
   ```

3. **Access Production Instance**
   - URL: `http://<your_server_ip>:9000`
   - Default credentials: `admin` / `admin`

## Verification and Testing

### Plugin Installation Verification

1. **Check Plugin Installation**
   - Log into SonarQube web interface
   - Navigate to **Administration > Marketplace > Installed**
   - Verify **SecAI** plugin is listed

2. **Test Analysis**[2]
   ```bash
   # Using Maven
   mvn clean verify sonar:sonar \
     -Dsonar.projectKey=test-project \
     -Dsonar.host.url=http://your-server-ip:9000 \
     -Dsonar.login=your-token
   ```

### Troubleshooting

**Common Issues and Solutions:**

- **Container startup failures:** Check logs with `sudo docker compose logs`
- **Plugin not loading:** Verify JAR file placement with `docker exec -it sonarqube ls /opt/sonarqube/extensions/plugins`
- **Database connection issues:** Ensure PostgreSQL container is running and accessible
- **API key errors:** Verify environment files contain valid API keys
- **Port conflicts:** Ensure ports 80 (TCP), 9000 and 8000 are available

**Health Checks:**
```bash
# Check all containers
sudo docker compose ps

# Check SonarQube logs
sudo docker compose logs sonarqube

# Check database connectivity
sudo docker compose logs db

# Check Flask application
sudo docker compose logs flaskapp
```

### Maintenance

**Regular Maintenance Tasks:**

- Update plugin JAR files when new versions are released
- Monitor disk usage for SonarQube data volumes
- Rotate API keys according to security policies
- Update base Docker images for security patches

**Backup Procedures:**
```bash
# Backup SonarQube data
sudo docker compose exec db pg_dump -U sonar sonar > backup.sql

# Backup volumes
sudo docker run --rm -v sonarqube_data:/data -v $(pwd):/backup alpine tar czf /backup/sonarqube_backup.tar.gz /data
```

## Additional Resources

### Docker Resources
- **Docker Official Website**: [https://www.docker.com/](https://www.docker.com/)
- **Docker Installation Guide**: [https://docs.docker.com/get-docker/](https://docs.docker.com/get-docker/)
- **Docker Compose Documentation**: [https://docs.docker.com/compose/](https://docs.docker.com/compose/)
- **SonarQube Docker Hub**: [https://hub.docker.com/_/sonarqube](https://hub.docker.com/_/sonarqube)
- **PostgreSQL Docker Hub**: [https://hub.docker.com/_/postgres](https://hub.docker.com/_/postgres)

### SonarQube Resources
- **SonarQube Official Documentation**: [https://docs.sonarqube.org/](https://docs.sonarqube.org/)
- **SonarQube Plugin Development**: [https://docs.sonarqube.org/latest/extend/developing-plugin/](https://docs.sonarqube.org/latest/extend/developing-plugin/)