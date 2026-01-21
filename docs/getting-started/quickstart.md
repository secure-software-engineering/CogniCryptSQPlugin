# Quick Start: SonarQube Analysis

This guide covers two methods to perform SonarQube analysis on your projects.

## Local Development Setup

For local development and testing, use this streamlined approach.

### Quick Local Setup

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd SonarQubePlugin
   ```

2. **Build the Plugin**
   ```bash
   mvn clean package -DskipTests
   ```
   The plugin JAR will be created at `target/sonar-secai-plugin-1.0.0.jar`

3. **Start Development Environment**
   ```bash
   docker-compose -f dockerfiles/docker-compose-development.yml up -d
   ```

4. **Deploy Plugin to Development Instance**
   ```bash
   chmod +x deploy-sonarqube-plugin.sh
   ./deploy-sonarqube-plugin.sh
   ```

5. **Access Local SonarQube**
   - URL: http://localhost:9000
   - Default credentials: `admin` / `admin`

### Local Development Analysis

Once your development environment is running, you can analyze projects using multiple methods.

**CLI Analysis (Maven)**
```bash
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=your-project-key \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=your-token
```

**IntelliJ IDEA Plugin**
- Install SonarLint plugin
- Connect to `http://localhost:9000`
- Bind your project for real-time analysis


## Method 1: CLI Analysis

### Prerequisites
- SonarQube server running (localhost:9000)
- Maven or Gradle project
- SonarQube Scanner CLI installed

### Steps

1. **Generate Project Token**
   ```bash
   # Login to SonarQube at http://localhost:9000
   # Go to My Account > Security > Generate Token
   ```

2. **Run Analysis (Maven)**
   ```bash
   mvn clean verify sonar:sonar \
     -Dsonar.projectKey=your-project-key \
     -Dsonar.host.url=http://localhost:9000 \
     -Dsonar.login=your-token
   ```

3. **Run Analysis (Gradle)**
   ```bash
   ./gradlew sonarqube \
     -Dsonar.projectKey=your-project-key \
     -Dsonar.host.url=http://localhost:9000 \
     -Dsonar.login=your-token
   ```

4. **Using Scanner CLI**
   ```bash
   sonar-scanner \
     -Dsonar.projectKey=your-project-key \
     -Dsonar.sources=. \
     -Dsonar.host.url=http://localhost:9000 \
     -Dsonar.login=your-token
   ```

## Method 2: IntelliJ IDEA Plugin

### Installation

1. **Install Plugin**
   - Go to `File > Settings > Plugins`
   - Search for "SonarLint"
   - Install and restart IDE

### Configuration

1. **Connect to SonarQube**
   - Go to `File > Settings > Tools > SonarLint`
   - Click "+" to add SonarQube connection
   - Enter server URL: `http://localhost:9000`
   - Add your token

2. **Bind Project**
   - Right-click project in Project Explorer
   - Select `SonarLint > Bind to SonarQube`
   - Choose your server and project

### Usage

1. **Real-time Analysis**
   - Issues appear automatically as you code
   - Check SonarLint tool window for details

2. **Manual Analysis**
   - Right-click file/folder
   - Select `SonarLint > Analyze with SonarLint`

3. **View Results**
   - Open SonarLint tool window (bottom panel)
   - Click issues to navigate to code

## Quick Commands Reference

| Action | CLI Command | IntelliJ Shortcut |
|--------|-------------|-------------------|
| Analyze current file | `sonar-scanner -Dsonar.sources=filename` | Right-click > Analyze |
| View issues | Check web UI | SonarLint tool window |
| Update rules | Restart scanner | Sync project binding |

## Troubleshooting

- **CLI**: Ensure Maven/Gradle wrapper has execute permissions
- **Plugin**: Verify SonarQube server is accessible from IDE
- **Both**: Check project token has analysis permissions