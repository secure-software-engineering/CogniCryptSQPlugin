# *SecAI* Plugin for SonarQube

This is a short guide on how to install the *SecAI* custom plugin.

## Installing the *SecAI* plugin

1. Add the plugin jar to the plugin folder:
    - With a SonarQube docker: If you used our docker compose file for your server there should be a `plugins` folder in the base directory that is connected directly to the correct location inside the container. Copy the jar into this folder.
    
        Alternatively, use the command below to move the file to `/opt/sonarqube/extensions/plugins` (on the running container `sonarqube`):

        ```bash
        docker cp ./secai-plugin-1.2.0.jar sonarqube:/opt/sonarqube/extensions/plugins
        ```
    - With SonarQube installed from a zip file: Locate the `extensions/plugins` folder inside your SonarQube distribution and move the jar into it.
2. Restart the SonarQube instance for the changes to take effect. You should receive a warning about third-party plugins and the plugin should be listed under **Administration > Marketplace > Plugins > Installed**.

## Automated Plugin Deployment 

If you intend to further develop this plugin, you can use the provided script (`deploy-sonarqube-plugin.sh`) for automating the deployment of the *SecAI* plugin to a SonarQube docker container. 

The script first builds the plugin jar using Maven. Therefore, if you are intending to use the plugin as is, it is recommended to simply download the compiled plugin from the [release page](https://github.com/secure-software-engineering/CogniCryptSQPlugin/releases).

At the start of the file there is a set of variables you may need to adjust to your setup, though the `PLUGIN_SOURCE_DIR` and `SONARQUBE_PLUGIN_DIR` should remain unchanged in most cases.

```bash
# Variables
DOCKER_CONTAINER_NAME="sonarqube"   # Name of your SonarQube container
PLUGIN_SOURCE_DIR="./target"        # plugin target directory
JAR_FILE="secai-plugin-1.2.0.jar"  # JAR name of plugin JAR
SONARQUBE_PLUGIN_DIR="/opt/sonarqube/extensions/plugins"
```

Everytime changes are made, simply run following command to automatically copy the new plugin jar into the docker container and restart the `sonarqube` container.

``` bash
./deploy-sonarqube-plugin.sh 
```
