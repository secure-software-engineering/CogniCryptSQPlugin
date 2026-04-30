# Setup

*SecAI* consists of two components:

1. A Flaskapp for the *AIFix* and *Confidence Score* features.
2. A plugin for a SonarQube server.

Before proceeding with the installation check out the [prerequisites](prerequisites.md).

## Prepare Files for Installation

Download the following files from the [release page](https://github.com/secure-software-engineering/CogniCryptSQPlugin/releases):
- zip archive: `secai-for-exist-sq` if you already have a SonarQube server, `secai-for-new-sq` if you intend to create a new one
- *SecAI* plugin jar

Unzip the archive on the intended host machine in a location that all administrators can access.

The resulting file structure should look like this for `secai-for-new-sq`:

```
/secai-for-new-sq/
├── Flaskapp/
│   ├── aifix/
│   │   ┊┄┄ # python files and additional folders
│   │   └── .env
│   ├── confidence/
│   │   └┄┄ # python files and model files
│   ├── .env
│   ├── Dockerfile
│   ├── gunicorn.conf.py
│   ├── main.py
│   └── requirements.txt
├── nginx/
│   └── default.conf.template
├── docker-compose.yml
└── Dockerfile
```

For `secai-for-exist-sq` the only difference should be that there is no `Dockerfile` at the top level, though the contents of the `docker-compose.yml` are also slightly different.

In the file `Flaskapp/aifix/.env`:
- Replace the placeholders for the API keys of the LLMs you intend to use.
- Unless you are hosting all components **including the projects to analyse** on the same machine, change the `FLASK_IP` to the IP address of the host machine

> **Security Note**: Never commit `.env` files to version control. Add them to your `.gitignore` file (or equivalent). At most, manually add a sample file with placeholders as a hint for new users.

## Configure SonarQube

How to configure your SonarQube instance depends on whether you are creating a new SonarQube server or extending an existing SonarQube instance.

### For a new SonarQube server

This setup uses the embedded SonarQube database. However, this makes it impossible to update to newer SonarQube versions as the database cannot be migrated. This is why the server configuration in the `docker-compose.yml` is **not meant for production use**. Please **adjust** the configuration to your needs using the [official SonarQube documentation](https://docs.sonarsource.com/sonarqube-server/server-installation). You may also need to change the image used in the `Dockerfile`.

### Extending an existing SonarQube server

Add the file `Flaskapp/aifix/.env` through the `env-file` attribute of your docker compose file or docker run command, or, if not using docker, define the environment variables on your host machine. If you intend to use the *Code Generation* feature you will also need a Java compiler.

## Create **Docker** Containers

Run the following command within the unzipped directory. You may have to use `sudo` for admin permissions.

```bash
docker compose up -d --build
```

This builds and runs two docker containers `nginx` and `flaskapp`. If you are creating a new SonarQube server at the same time, a `sonarqube` docker should also now be running.

## Installing the *SecAI* plugin

Add the plugin jar to the plugin folder:
- **With a SonarQube docker:** If you used our docker compose file for your server there should be a `plugins` folder in the base directory that is connected directly to the correct location inside the container. Copy the jar into this folder.

    Alternatively, use the command below to move the file to `/opt/sonarqube/extensions/plugins` (on the running container `sonarqube`):

    ```bash
    docker cp ./secai-plugin-1.1.0.jar sonarqube:/opt/sonarqube/extensions/plugins
    ```

    > Note: On Windows the **jar path** should use backslash ("\\") instead of forward slash ("/").
- **With SonarQube installed [from a zip file](https://docs.sonarsource.com/sonarqube-server/server-installation/from-zip-file):** Locate the `extensions/plugins` folder inside your SonarQube distribution and move the jar into it.

Restart the SonarQube instance for the changes to take effect. You should receive a warning about third-party plugins and the plugin should be listed under **Administration > Marketplace > Plugins > Installed**.
 
Afterwards, you can run your [first analysis](first-analysis.md).