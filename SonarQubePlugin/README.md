# SonarQubePlugin
Repository for the SecAI project group for the SonarQube plugin development


This is a short guide on how to setup Sonarqube on your system, and install the "SecAI" custom plugin.

1. **SonarQube installation**
There are two methods to install SonarQube on your system, either from a ZIP file or via Docker.
    Method 1:
    - Go to this link https://www.sonarsource.com/products/sonarqube/downloads/
    - Download the community edition by giving the mail-id.
    - Extract the files
    - Go to bin->select the environment->run the startsonar file.
    
    Method 2:
    - Download Docker https://www.docker.com/products/docker-desktop/
    - Once downloaded run this command in CMD  docker run -d --name sonarqube -e             SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true -p 9000:9000 sonarqube:latest
    - Wait for some time depending on your machine resources and visit
    http://localhost:9000/default 
    user: admin
    password: admin 

2. **Creating a project in SonarQube**
- Once the local host is running then go to projects and select create a local project.
- Select the display name and project key (I put “secai” in both),.
- Click next and select use global settings.
- And click create project.
- Select analysis method as locally for now.
- In next step select generate project token and expiration as no expiration. Click next
- Then you will get a command to run for project on which you want to analyse the code.
    NOTE: Run this command in cmd/terminal of the host machine not from the IDE terminal (It will not work)
    - Here If you get an error when you first run that command run the below command
    mvn clean install org.sonarsource.scanner.maven:sonar-maven-plugin:3.5.0.1254:sonar
    - then restart the cmd --- again run the provided command from SonarQube.
- Once the analysis done you will get report on the webpage. 

3. **Running the SecAI plugin**
- Clone this github repository
- Run “mvn clean package"
- Go to /target/sonar-plugin-example-x.x.x.jar and copy that file into the directory -> sonarqube server file extensions/plugins/
    - If you are using Docker then 
    docker cp /path/to/your/generated/jar/file {docker_containerID}:/opt/sonarqube/extensions/plugins/
    {docker_containerID} : run docker ps  to get Id of the running container
- Restart the SonarQube server
- Your plugin should be working now!


# Automated Plugin Deployment 
Here provided a script for automating the deployment of the Sonarqube Plugin using Docker.

### Usage
Everytime the changes being made, just run following commands to automate the deployment process:
``` bash
./deploy-sonarqube-plugin.sh 
```
