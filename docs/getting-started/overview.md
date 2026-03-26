# Overview

*SecAI* consists of two components:

1. A Flaskapp for the *AIFix* and *Confidence Score* features.
2. A plugin for a SonarQube server.

Before proceeding with the installation check out the [prerequisites](prerequisites.md).

1. Download the following files from the [release page](https://github.com/secure-software-engineering/CogniCryptSQPlugin/releases):
    - zip archive: `secai-for-exist-sq` if you already have a SonarQube server, `secai-for-new-sq` if you intend to create a new one
    - *SecAI* plugin jar
2. Unzip the archive on the intended host machine in a location that all administrators can access.
3. In the file `Flaskapp/aifix/.env`:
    - Replace the placeholders for the API keys of the LLMs you intend to use.
    - Unless you are hosting all components **including the projects to analyse** on the same machine, change the `FLASK_IP` to the IP address of the host machine
4. Configure SonarQube:
    - **For a new SonarQube server:** The server configuration in the `docker-compose.yml` is not meant for production use. Please adjust the configuration to your needs using the [official SonarQube documentation](https://docs.sonarsource.com/sonarqube-server/server-installation). You may also need to change the image used in the `Dockerfile`.
    - **If you are extending an existing SonarQube server:** Add the file `Flaskapp/aifix/.env` through the `env-file` attribute of your docker compose file or docker run command, or, if not using docker, define the environment variables on your host machine
5. Run the following command within the unzipped directory. You may have to use `sudo` for admin permissions.

    ```bash
    docker compose up -d --build
    ```

    This creates two docker containers `nginx` and `flaskapp`. If you are creating a new SonarQube server at the same time, a `sonarqube` docker should also now be running.
6. Install the *SecAI* plugin:
    - Add the plugin jar to the plugin folder:
        - With a SonarQube docker: If you used our docker compose file for your server there should be a `plugins` folder in the base directory that is connected directly to the correct location inside the container. Copy the jar into this folder.
        
            Alternatively, use the command below to move the file to `/opt/sonarqube/extensions/plugins` (on the running container `sonarqube`):

            ```bash
            docker cp ./secai-plugin-1.1.0.jar sonarqube:/opt/sonarqube/extensions/plugins
            ```
        - With SonarQube installed from a zip file: Locate the `extensions/plugins` folder inside your SonarQube distribution and move the jar into it.
    - Restart the SonarQube instance for the changes to take effect. You should receive a warning about third-party plugins and the plugin should be listed under **Administration > Marketplace > Plugins > Installed**.
 
Afterwards, you can run your [first analysis](first-analysis.md).