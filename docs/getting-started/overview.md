# Overview

*SecAI* consists of two components:

1. A Flaskapp for the *AIFix* and *Confidence Score* features. It can be installed in a docker container or on a host machine with Linux as the opertaing system.
2. A plugin for a SonarQube server.

Before proceeding with the installation check out the [prerequisites](prerequisites.md).

1. Download the following files:
    - zip archive: `secai-for-exist-sq` if you already have a SonarQube server, `secai-for-new-sq` if you intend to create a new one
    - SecAI plugin jar
2. Unzip the archive on the intended host machine in a location that all administrators can access.
3. In the file `Flaskapp/aifix/.env`:
    - Replace the placeholders for the API keys of the LLMs you intend to use.
    - Unless you are hosting all components including the projects to analyse on the same machine, change the `FLASK_IP` to the IP address of the host machine
4. Configure SonarQube:
    - **For a new SonarQube server:** The server configuration in the `docker-compose.yml` is not meant for production use. Please adjust the configuration to your needs using the [official SonarQube documentation](https://docs.sonarsource.com/sonarqube-server/server-installation)
    - **If you are extending an existing SonarQube server:** Add the file `Flaskapp/aifix/.env` through the `env-file` attribute of your docker compose file or docker run command, or, if not using docker, define the environment variables on your host machine
    - Add the plugin jar to the plugin folder:
        - In docker: Use [`docker container cp`](https://docs.docker.com/reference/cli/docker/container/cp/) to move the file to `/opt/sonarqube/extensions/plugins` (on the running container). Alternatively, if you used our docker compose file for your server there should be a `plugins` folder in the base directory that is connected directly to the mentioned location inside the container.
        - With SonarQube installed from a zip file: Locate the `extensions/plugins` folder inside your SonarQube distribution and move the jar into it.
    - Restart the SonarQube instance for the changes to take effect. You should receive a warning about third-party plugins and the plugin should be listed under **Administration > Marketplace > Plugins > Installed**.
5. Refer to the [this guide](first-analysis.md) on how to set up the analysis using the SecAI plugin.
 
Afterwards, you can run your [first analysis](first-analysis.md).