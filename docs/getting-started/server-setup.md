
# Complete SonarQube SECAI Plugin Server Setup & Deployment Guide

Please check the [prerequisites](prerequisites.md) before proceeding.

You will also need the following from the [release page](https://github.com/secure-software-engineering/CogniCryptSQPlugin/releases) of our GitHub repository:

- SecAI plugin jar: `sonar-secai-plugin-1.0.0.jar` (or later)
- `secai-for-new-sq-1.0.0.zip`

Unpack the `secai-for-new-sq-1.0.0.zip` file in the location where you intend to install your SonarQube server. This location should be accessible to your administrators. The resulting file structure should look like this:

```
/secai-for-new-sq/
├── AIFix/
│   ┊┄┄ # python files and additional folders
│   ├── .env
│   ├── Dockerfile
│   └── requirements.txt
├── Confidence/
│   ┊┄┄ # python files and model files
│   ├── Dockerfile
│   └── requirements.txt
├── docker-compose.yml
└── Dockerfile
```

This comprehensive guide covers both local development setup and production server deployment for the SonarQube SECAI plugin stack.

> **Important:** The SonarQube server created during this setup is very basic and does not consider aspects such as database installation. Please check the [official server installation instructions](https://docs.sonarsource.com/sonarqube-server/server-installation) for additional necessary components and modify the `docker-compose.yml`accordingly.

## Environment Configuration

Environment files store sensitive configuration data like API keys. Update these files to configure the services properly.

Currently, the file `./AIFix/.env` contains placeholders for the API keys. Replace `your_openai_api_key_here` and `your_google_api_key_here` with your [own keys](prerequisites.md#api-keys).

> **Security Note**: Never commit `.env` files to version control. Add them to your `.gitignore` file. At most, manually add a sample file with placeholders as a hint for new users.

> **Note:** If your SonarQube instance requires additional environment variables you can simply add an additional `.env`-file to the `sonarqube` service in the `docker-compose.yml`.

## Deployment

Inside the `secai-for-new-sq` folder run the following commands with administrator rights:

1. **Build and Start Services**: \
   
   ```bash
   docker compose build
   docker compose up -d
   ```

2. **Monitor Startup**
   ```bash
   docker compose logs -f sonarqube
   ```

You can then access your new SonarQube instance at `http://<your_server_ip>:9000` using the default credentials `admin` / `admin`. Final steps to start your first analysis can be found [here](first-analysis.md).

## Troubleshooting

**Common Issues and Solutions:**

- **Container startup failures:** Check logs with `sudo docker compose logs`
- **Plugin not loading:** Verify JAR file placement with `docker exec -it sonarqube ls /opt/sonarqube/extensions/plugins`
- **Database connection issues:** Ensure PostgreSQL container is running and accessible
- **API key errors:** Verify environment files contain valid API keys
- **Port conflicts:** Ensure ports 80 (TCP), 9000 and 8000 are available

**Health Checks:**
```bash
# Check all containers
sudo docker compose ps

# Check SonarQube logs
sudo docker compose logs sonarqube

# Check database connectivity
sudo docker compose logs db

# Check Flask application
sudo docker compose logs flaskapp
```

## Maintenance

**Regular Maintenance Tasks:**

- Update plugin JAR files when new versions are released
- Monitor disk usage for SonarQube data volumes
- Rotate API keys according to security policies
- Update base Docker images for security patches

**Backup Procedures:**
```bash
# Backup SonarQube data
sudo docker compose exec db pg_dump -U sonar sonar > backup.sql

# Backup volumes
sudo docker run --rm -v sonarqube_data:/data -v $(pwd):/backup alpine tar czf /backup/sonarqube_backup.tar.gz /data
```

## Additional Resources

### Docker Resources
- **Docker Official Website**: [https://www.docker.com/](https://www.docker.com/)
- **Docker Installation Guide**: [https://docs.docker.com/get-docker/](https://docs.docker.com/get-docker/)
- **Docker Compose Documentation**: [https://docs.docker.com/compose/](https://docs.docker.com/compose/)
- **SonarQube Docker Hub**: [https://hub.docker.com/_/sonarqube](https://hub.docker.com/_/sonarqube)
- **PostgreSQL Docker Hub**: [https://hub.docker.com/_/postgres](https://hub.docker.com/_/postgres)

### SonarQube Resources
- **SonarQube Official Documentation**: [https://docs.sonarqube.org/](https://docs.sonarqube.org/)
- **SonarQube Plugin Development**: [https://docs.sonarqube.org/latest/extend/developing-plugin/](https://docs.sonarqube.org/latest/extend/developing-plugin/)