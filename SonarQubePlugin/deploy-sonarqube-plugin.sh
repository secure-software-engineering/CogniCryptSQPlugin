#!/bin/bash

WORKINGDIR=$(pwd)
# After restarting Docker we need to deploy our Sonarqube Plugin within it, script will be changed a bit for that..

# Variables
DOCKER_CONTAINER_NAME="sonarqube"   # Name of your SonarQube container
PLUGIN_SOURCE_DIR="./target"        # plugin target directory
JAR_FILE="sonar-secai-plugin-1.0.0.jar"  # JAR name of plugin JAR
SONARQUBE_PLUGIN_DIR="/opt/sonarqube/extensions/plugins"  # NEEDS to be UPDATED: Plugin directory inside the container

# Build the JAR file
echo "Building the JAR file using Maven..."
cd "$(dirname "$PLUGIN_SOURCE_DIR")" || { echo "Error: Directory $PLUGIN_SOURCE_DIR does not exist!"; exit 1; }
mvn clean package -DskipTests
	
if [ $? -ne 0 ]; then
  echo "Error: Maven build failed. Please check your Maven project setup."
  exit 1
fi

# Verify the JAR file exists
if [ ! -f "$PLUGIN_SOURCE_DIR/$JAR_FILE" ]; then
  echo "Error: JAR file $PLUGIN_SOURCE_DIR/$JAR_FILE does not exist after Maven build."
  exit 1
fi

echo "JAR file built successfully: $PLUGIN_SOURCE_DIR/$JAR_FILE"

docker ps --filter "name=sonarqube" --filter "status=running" | grep -q "sonarqube"
if [ $? -ne 0 ]; then
    echo "sonarqube is NOT running. e.g. execute the following command before running this script:"
    echo "docker run -d --name sonarqube -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true -p 9000:9000 sonarqube:latest"
    exit 1;
fi

# Copy the JAR file to the Docker container
echo "Copying $JAR_FILE to $SONARQUBE_PLUGIN_DIR inside the Docker container..."
docker cp "$PLUGIN_SOURCE_DIR/$JAR_FILE" "$DOCKER_CONTAINER_NAME:$SONARQUBE_PLUGIN_DIR"

if [ $? -ne 0 ]; then
  echo "Error: Failed to copy the JAR file into the Docker container."
  exit 1
fi

# Restart the Docker container
echo "Restarting the SonarQube Docker container..."
docker stop "$DOCKER_CONTAINER_NAME"
docker start "$DOCKER_CONTAINER_NAME"

if [ $? -ne 0 ]; then
  echo "Error: Failed to restart the Docker container."
  exit 1
fi

echo "Deployment complete! $JAR_FILE has been built, copied to $SONARQUBE_PLUGIN_DIR, and the Docker container has been restarted."

echo "Starting LSP server for quick fix!"

# Update: path of LSP_Server, where POM.xml resides!
LSP_DIR="./Lsp_server/lsptest"
LSP_JAR="lsptest-1.0-SNAPSHOT.jar" 


echo "Waiting for SonarQube to be fully up...!"
# Waiting for approx 40 sec. for sonarqube to be operational!
sleep 40


echo "Building the subproject with Maven..."
cd "$LSP_DIR" || { echo "Error: Directory $LSP_DIR does not exist!"; exit 1; }
mvn clean package

if [ $? -ne 0 ]; then
  echo "Error: Maven build failed for subproject. Please check your Maven project setup."
  exit 1
fi

cd "$WORKINGDIR"

# Step: Run the jar from subproject's target
if [ ! -f "$LSP_DIR/target/$LSP_JAR" ]; then
  echo "Error: Subproject JAR file $LSP_DIR/target/$LSP_JAR does not exist after Maven build."
  exit 1
fi

echo "Running the subproject JAR..."
java -jar "$LSP_DIR/target/$LSP_JAR"
