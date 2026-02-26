# Install Flaskapp On Linux

Please check the [prerequisites](prerequisites.md) before proceeding.

> **Important:** This guide cannot be executed on Windows as multiple components do not support this operating system.

You will also need the following from the [release page](https://github.com/secure-software-engineering/CogniCryptSQPlugin/releases) of our GitHub repository:

- SecAI plugin jar: `sonar-secai-plugin-1.1.0.jar` (or later)
- `secai-for-existing-sq-1.1.0.zip`

Unpack the `secai-for-existing-sq-1.1.0.zip` file in the location where you intend to host the additional components. This location should be accessible to your administrators. The resulting file structure should look like this:

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

As we are not using docker the files `docker-compose.yml` and `Flaskapp/Dockerfile` can be safely deleted.

## Server Name

The server name given in line 7 of the file `nginx/default.conf` is how the *SecAI* plugin installed on the SonarQube server is meant to reach the flask app backend.

If the flask app is installed on the same host machine as your SonarQube instance, no changes are necessary as the plugin will default to the localhost IP of `127.0.0.1`.

Otherwise, you sill need to change the server name, e.g. to the IP address of the machine hosting the flask app. The change will need to be added to the SonarQube server as well by setting the environment variable **FLASK_IP** on its host. PLease see [here](TODO) for details.

## Environment Variables

In order to use the *AIFix* feature the following environment variables need to be set:

- **OPEN_AI_API_KEY** for ChatGPT
- **GOOGLE_API_KEY** for Gemini

Whether or not you wish to provide API keys for both or only one depends on which LLMs you intend to use.

// TODO: flask debug

Please turn to online guides on creating environment variables for your Linux distribution.

> **Note:** Changes made to an environment variable with commands often revert when the console from which the command was sent is closed. Make sure to set the variables permanently or create an easily reusable script.

## Install Requirements

It is assumed that *Python 3.11+* and *pip* are installed.

### requirements.txt

Change your working directory to `Flaskapp` or adjust the path accordingly.

Run the following command to install the dependencies of the flask app:

```bash
pip install --no-cache-dir -r requirements.txt
```

### nginx

Install [nginx](https://nginx.org/) by running the following commands:

```bash
sudo apt update
sudo apt install nginx
```

After installing there should be a folder `/etc/nginx/`. Move the configuration file `secai-for-existing-sq/nginx/default.conf` to `/etc/nginx/conf.d/default.conf`. Ensure that the [server name](#server-name) in the file was updated as necessary.

## Run gunicorn

// TODO
// adjust number of workers if necessary

## Run nginx

// TODO