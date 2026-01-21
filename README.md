# CogniCryptSQPlugin

This repository combines the different components of the *SecAI* SonarQube plugin, which integrates [*CogniCrypt<sub>SAST</sub>*](https://github.com/CROSSINGTUD/CryptoAnalysis).

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

## Components

In order to use all implemented functionality, multiple components have to be combined.

### SonarQubePlugin

The directory [SonarQubePlugin](SonarQubePlugin) contains the code of the actual *SecAI* plugin. In order to add the plugin to a SonarQube instance a jar of this project is needed. For further details, consult the [documentation](#documentation). <!-- add actual docs links -->

### AIFix

Inside the [AIFix](AIFix) folder there is the code for the web server managing the *AIFix* extension. For further details, consult the [documentation](#documentation). <!-- add actual docs links -->

### Confidence

The directory [Confidence](Confidence) contains the backend for the *confidence score* extension. For further details, consult the [documentation](#documentation). <!-- add actual docs links -->

> [!NOTE]
> This folder contains only the trained model used for the calculation of the confidence score. The code for training the model  can be found in [this repository](https://github.com/secure-software-engineering/SecAI_FalsePositiveDetector).