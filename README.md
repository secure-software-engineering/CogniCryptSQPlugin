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

To quickly download the documentation instead of cloning the repository check out the releases.

---

## Components

In order to use all implemented functionality, multiple components have to be combined.

### SonarQubePlugin

The directory [SonarQubePlugin](SonarQubePlugin) contains the code of the actual *SecAI* plugin. In order to add the plugin to a SonarQube instance a jar of this project is needed. For further details, consult the [documentation](#documentation). <!-- add actual docs links -->

### Flaskapp

The features *AIFix* and *Confidence Score* require an additional python backend. This directory contains a [Flask](https://github.com/pallets/flask/) app implementing the necessary functionality.

> [!NOTE]
> The folder [`Flaskapp/confidence`](Flaskapp/confidence) contains only the trained model used for the calculation of the confidence score. The code for training the model can be found in [this repository](https://github.com/secure-software-engineering/SecAI_FalsePositiveDetector).

---

## Releases

After running `mvn clean package -DskipTests` in the root directory a folder `release` will be created with the following contents:

- `secai-for-existing-sq-<version>.zip`: zip archive containing the source code for *AIFix* and *Confidence Score* and the **docker compose** for just these containers
- `secai-for-new-sq-<version>.zip`: zip archive containing the source code for *AIFix* and *Confidence Score* and the **docker compose** for a new SonarQube setup
- `secai-docs-<version>.zip`: zip archive containing the Markdown files of the documentation

An up-to-date version of the plugin jar is generated in the `SonarQubePlugin/target/` directory.
