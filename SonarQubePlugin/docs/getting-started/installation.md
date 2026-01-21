# Installation & Deployment Guide

This guide explains how to build, run, and manually deploy the SecAI SonarQube Plugin using Docker Compose and the provided deployment script.

---

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and [Docker Compose](https://docs.docker.com/compose/install/) installed
- [Java 17+](https://adoptium.net/) and [Maven](https://maven.apache.org/install.html) installed
- Project cloned to your machine

---

## 1. Clone the Repository

```bash
git clone <repository-url>
cd SonarQubePlugin
```

---

## 2. Build the Plugin

```bash
mvn clean package -DskipTests
```

- The plugin JAR will be created at:  
  `target/sonar-secai-plugin-1.0.0.jar`

---

## 3. Start SonarQube with Docker Compose

```bash
docker-compose -f dockerfiles/docker-compose-development.yml up -d
```

- This will start SonarQube at [http://localhost:9000](http://localhost:9000)
- Default login: `admin` / `admin`

---

## 4. Deploy the SecAI Plugin

### 4.1. Update the Deployment Script

Open `deploy-sonarqube-plugin.sh` and ensure these variables match your setup:

```bash
DOCKER_CONTAINER_NAME="sonarqube"  # Should match the container_name in docker-compose-backup.yml
PLUGIN_SOURCE_DIR="./target"
JAR_FILE="sonar-secai-plugin-1.0.0.jar"
SONARQUBE_PLUGIN_DIR="/opt/sonarqube/extensions/plugins"
```

### 4.2. Make the Script Executable

```bash
chmod +x deploy-sonarqube-plugin.sh
```

### 4.3. Run the Deployment Script

```bash
./deploy-sonarqube-plugin.sh
```

- This will:
  - Build the plugin JAR (again, to ensure latest)
  - Copy it into the running SonarQube container
  - Restart the container to load the plugin

---

## 5. Verify Plugin Installation

1. Open [http://localhost:9000](http://localhost:9000)
2. Login as `admin` / `admin`
3. Go to **Administration > Marketplace > Installed**  
   You should see **SecAI** listed.

---

## 6. Stopping the Services

To stop and remove the containers:

```bash
docker-compose -f dockerfiles/docker-compose-development.yml down
```

---

## Troubleshooting

- **Check logs:**  
    ```bash
    docker-compose -f dockerfiles/docker-compose-development.yml logs
    ```
- **Check plugin deployment:**  
    ```bash
    docker exec -it sonarqube ls /opt/sonarqube/extensions/plugins
    ```
- **Restart SonarQube manually:**  
    ```bash
    docker restart sonarqube
    ```
- **Ports in use?**  
    Make sure port 9000 is free.

---

For advanced configuration, refer to the comments in `docker-compose-development.yml` or the