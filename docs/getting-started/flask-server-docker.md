# Install Flaskapp Using Docker

Please check the [prerequisites](prerequisites.md) before proceeding.

You will also need the following from the [release page](https://github.com/secure-software-engineering/CogniCryptSQPlugin/releases) of our GitHub repository:

- SecAI plugin jar: `sonar-secai-plugin-1.1.0.jar` (or later)
- `secai-for-existing-sq-1.1.0.zip`

The size of images built for the additional docker containers will be around 9 GB.

Unpack the `secai-for-existing-sq-1.1.0.zip` file in the location where you intend to install the additional components. This location should be accessible to your administrators. The resulting file structure should look like this:

```
/secai-for-existing-sq/
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
│   └── default.conf
└── docker-compose.yml
```

---

## Create Flaskapp Docker

The *AIFix* and *Confidence Score* features are installed in a *flaskapp* docker container separate from the main SonarQube server. An *nginx* docker is used as a reverse proxy.

### Environment Variables

In this guide we use `.env`-files to set environment variables in our docker container. The `.env` file inside the `Flaskapp/aifix` directory should look as follows:

```
# OpenAI API Configuration (ChatGPT)
OPEN_AI_API_KEY=your_openai_api_key_here

# Google API Configuration (Gemini)
GOOGLE_API_KEY=your_google_api_key_here

# Flask Configuration
FLASK_DEBUG=false
```

If you wish to use the cloud-based LLMs [ChatGPT](https://chatgpt.com/) and [Gemini](https://gemini.google.com/) you will have to replace the placeholder assigned to the respective environent variable with your [own API key](prerequisites.md#api-keys). This key will be used for every outgoing request.

// TODO: FLASK_IP

> **Security Note**: Never commit `.env` files to version control. Add them to your `.gitignore` file (or equivalent). At most, manually add a sample file with placeholders as a hint for new users.

If you intend to use the *Code Generation* feature (which is separate from the *AIFix* feature) the API keys set in this file must also be set on the SonarQube server. How you approach this depends on whether your SonarQube server was installed [from a `ZIP` file](https://docs.sonarsource.com/sonarqube-server/server-installation/from-zip-file) or [from a `Docker` image](https://docs.sonarsource.com/sonarqube-server/server-installation/from-docker-image).

#### SonarQube from a ZIP file

In this case how you set the system environment variables depends on the operating system of your host. Please refer to an online guide for instructions specific to the operating system of your host machine.

> **Note:** Changes made to an environment variable with commands often revert when the console from which the command was sent is closed. Make sure to set the variables permanently or create an easily reusable script.

#### SonarQube from Docker

If you are using `docker compose` to run your server you can simply add the `.env` file to the SonarQube service in your main `docker-compose.yml` (or `compose.yml`) using the `env_file` attribute.

If you are instead using the `docker run` command you can add the file using the `--env-file` flag.

For other methods of setting environment variables in Docker please refer to the official documentation for [docker compose](https://docs.docker.com/compose/how-tos/environment-variables/set-environment-variables/) and [docker run](https://docs.docker.com/reference/cli/docker/container/run/#env).

### Container Setup

Among the unpacked files in the `secai-for-existing-sq` directory there should be a file called `docker-compose-exist.yml` with the following contents:

```yml
services:
  nginx:
    image: nginx:1.29.5
    container_name: nginx
    volumes:
      - ./nginx/default.conf:/etc/nginx/conf.d/default.conf
    ports:
      - 80:80
    networks:
      - internal
    depends_on:
      - flaskapp
    restart: unless-stopped
    
  flaskapp:
    build: ./Flaskapp
    container_name: flaskapp
    env_file:
      - ./Flaskapp/aifix/.env
    volumes:
      - ./Flaskapp:/app
    networks:
      internal:
        aliases:
          - flask-app
    restart: unless-stopped

networks:
  internal:
```

If you are running SonarQube from a docker container and installing the new components on the same host machine, then they must share a [network](https://docs.docker.com/reference/compose-file/networks/) to communicate. 

If you did not specify a network in the `compose`-file or `docker run command` you can proceed with the file above as the default network is sufficient. Otherwise, you will have to add a new network as follows:

```yml
services:
  nginx:
    image: nginx:1.29.5
    container_name: nginx
    volumes:
      - ./nginx/default.conf:/etc/nginx/conf.d/default.conf
    ports:
      - 80:80
    networks:
      - internal
    depends_on:
      - flaskapp
    restart: unless-stopped
    
  flaskapp:
    build: ./Flaskapp
    container_name: flaskapp
    env_file:
      - ./Flaskapp/aifix/.env
    volumes:
      - ./Flaskapp:/app
    networks:
      internal:
        aliases:
          - flask-app
    restart: unless-stopped

networks:
  internal:
```

Add the same network to your SonarQube `docker compose` or add `--network=secai` to your `docker run` command. As adding the *SecAI* plugin itself will also require restarting your SonarQube docker you do not need to do it immediately.

After checking the network configuration (if necessary) you can start your docker containers by running the following command from within your `secai-for-existing-sq` directory:

```bash
docker compose up -d --build
```

Your *flaskapp* and *nginx* docker containers should now be built and then running. You can stop the containers by running `docker stop <container_name>`. To start them again use `docker start <container_name>`.

---

## Setting Up the SecAI Plugin

The final step is to add the actual plugin to the server and activate it. How you approach this depends on whether your SonarQube server was installed [from a `ZIP` file](https://docs.sonarsource.com/sonarqube-server/server-installation/from-zip-file) or [from a `Docker` image](https://docs.sonarsource.com/sonarqube-server/server-installation/from-docker-image).

### SonarQube from a ZIP file

Locate the directory containing your SonarQube distribution. Among other folders such as `elasticsearch` there should be a folder called `extensions`. Copy the SecAI plugin jar (`sonar-secai-plugin-1.1.0.jar`) to `extensions/plugins/`.

The plugin will only be detected by the server after a [restart](https://docs.sonarsource.com/sonarqube-server/server-installation/from-zip-file/starting-stopping-server). Then an administrator account can verify its presence under **Administration > Marketplace > Plugins > Installed**. 

> Note: The plugin will only be executed when at least one SonarQube rule from the `CogniCrypt Security Rules` repository is activated in the quality profile (**Project Settings > Quality Profiles**) of the project.

### SonarQube from Docker

Ensure that the server is running and copy the `sonar-secai-plugin-1.1.0.jar` file to `/opt/sonarqube/extensions/plugins`. If you are using Docker Desktop or an equivalent application with a graphical user interface you can simply drag and drop or upload the file. Otherwise, you can execute the following command:

```bash
docker cp ./sonar-secai-plugin-1.1.0.jar sonarqube:/opt/sonarqube/extensions/plugins
```

> Note: The above example assumes that your SonarQube docker container is named `sonarqube`. Additionally, on Windows the **jar path** should use backslash ("\\") instead of forward slash ("/").

The plugin will only be detected by the server after a restart. Then an administrator account can verify its presence under **Administration > Marketplace > Plugins > Installed**. 

> Note: The plugin will only be executed when at least one SonarQube rule from the `CogniCrypt Security Rules` repository is activated in the quality profile (**Project Settings > Quality Profiles**) of the project.