# CogniCryptSQPlugin

This repository combines the different components of the *SecAI* SonarQube plugin, which integrates [*CogniCrypt<sub>SAST</sub>*](https://github.com/CROSSINGTUD/CryptoAnalysis).

---

## Documentation

You can access the documentation of the plugin using the [*docs* folder](/docs). 

Alternatively, you can build the documentation using the following commands and open the index page in `/site/index.html`:

```bash
# Install dependencies (you only need to do this once)
pip install -r requirements.txt

# Build documentation
mkdocs build
```

Another method would be to serve the documentation locally. The output of the command will then provide a link to access the pages hosted on the localhost.

```bash
# Install dependencies (you only need to do this once)
pip install -r requirements.txt

# Serve documentation locally
mkdocs serve
```

To quickly download the documentation instead of cloning the repository check out the [releases](https://github.com/secure-software-engineering/CogniCryptSQPlugin/releases).

---

## Releases

After running the following command in the root directory:

```bash
mvn clean package -DskipTests
```

A folder `release` will be created with the following contents:

- `secai-for-existing-sq-<version>.zip`: zip archive containing the source code for *AIFix* and *Confidence Score* and the **docker compose** for just these containers
- `secai-for-new-sq-<version>.zip`: zip archive containing the source code for *AIFix* and *Confidence Score* and the **docker compose** for a new SonarQube setup
- `secai-docs-<version>.zip`: zip archive containing the Markdown files of the documentation

An up-to-date version of the plugin jar is generated in the `SonarQubePlugin/target/` directory.

---

## Setup

Before proceeding with the installation check out the [prerequisites](docs/getting-started/prerequisites.md).

1. Download the following files from the [release page](https://github.com/secure-software-engineering/CogniCryptSQPlugin/releases):
    - zip archive: `secai-for-exist-sq` if you already have a SonarQube server, `secai-for-new-sq` if you intend to create a new one
    - *SecAI* plugin jar

    Alternatively, you can generate the release files yourself using the [above instructions](#releases)
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
 
Afterwards, you can run your [first analysis](docs/getting-started/first-analysis.md).

---

## Components

In order to use all implemented functionality, multiple components have to be combined.

### SonarQubePlugin

The directory [SonarQubePlugin](SonarQubePlugin) contains the code of the actual *SecAI* plugin. In order to add the plugin to a SonarQube instance a jar of this project is needed. For further details, consult the [documentation](#documentation). <!-- add actual docs links -->

### Flaskapp

The features *AIFix* and *Confidence Score* require an additional python backend. This directory contains a [Flask](https://github.com/pallets/flask/) app implementing the necessary functionality.

> [!NOTE]
> The folder [`Flaskapp/confidence`](Flaskapp/confidence) contains only the trained model used for the calculation of the confidence score. The code for training the model can be found in [this repository](https://github.com/secure-software-engineering/SecAI_FalsePositiveDetector).

### AIFix

Inside the [AIFix](AIFix) folder there is the code for the web server managing only the *AIFix* extension. For further details, consult the [documentation](#documentation). <!-- add actual docs links -->

> [!WARNING]
> *AIFix* was combined with *Confidence Score* in a new directory [Flaskapp](Flaskapp). The code in the [AIFix directory](AIFix) is deprecated and will be removed.

### Confidence

The directory [Confidence](Confidence) contains the backend for only the *confidence score* extension. For further details, consult the [documentation](#documentation). <!-- add actual docs links -->

> [!NOTE]
> This folder contains only the trained model used for the calculation of the confidence score. The code for training the model  can be found in [this repository](https://github.com/secure-software-engineering/SecAI_FalsePositiveDetector).

> [!WARNING]
> *Confidence Score* was combined with *AIFix* in a new directory [Flaskapp](Flaskapp). The code in the [Confidence directory](Confidence) is deprecated and will be removed.