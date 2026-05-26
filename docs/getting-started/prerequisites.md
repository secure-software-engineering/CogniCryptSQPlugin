# Prerequisites

Some prerequisites must be fulfilled before you can proceed with the installation.

---

## Hardware Requirements

If you intend to set up a new SonarQube server instead of using an existing one you can check [here](https://docs.sonarsource.com/sonarqube-server/server-installation/server-host-requirements) for the exact hardware requirements. In general, it is recommended to have at least 4 GB RAM. They also suggest at least 30 GB disk space, though this depends on how much code you intend to analyze; the size of the docker image itself is less than 2 GB.

For the *SecAI* plugin two additional docker containers will be installed. Combined, the two images will require roughly 9 GB of disk space.

---

## Software Requirements

### `Docker` and `Docker Compose`

The components required for the features *Confidence Score* and *AIFix* are installed as docker containers using docker compose. You can check the [official docker documentation](https://docs.docker.com/get-docker/) for detailed installation instructions.

### Java

The *Code Generation* feature requires Java compiler of Java 17+ to verify the generated code. If you are using this guide to set up a new SonarQube server then this is handled automatically. However, if you are expanding an existing SonarQube instance this step is necessary to be able to use this feature, as the JRE included in the standard SonarQube installation does not contain a compiler. 

Where to install Java depends on how your SonarQube instance is installed:

1. If you are running SonarQube [from a **ZIP** file](https://docs.sonarsource.com/sonarqube-server/server-installation/from-zip-file) then you can simply install Java 17+ on your host machine. <!-- TODO: check if JAVA_HOME is needed -->
2. If you are using [a **Docker** image](https://docs.sonarsource.com/sonarqube-server/server-installation/from-docker-image) then adding a Java compiler afterwards will be difficult as only files located in volumes persist between container restarts.

---

## API Keys

In order to use the *AIFix* and *Code Generation* features you API keys. If you only intend to use one of the two platforms you do not need keys for both. You will need to copy the generated keys during the *SecAI* setup.

### OpenAI API Key

Visit [https://platform.openai.com/api-keys](https://platform.openai.com/api-keys) and sign in to your OpenAI account.
Click **Create new secret key**.

### Google API Key

Visit [Google Cloud Console](https://console.cloud.google.com/). Create a new project or select existing one. Enable required APIs (e.g., Google AI Platform). Go to **Credentials → Create Credentials → API Key**.