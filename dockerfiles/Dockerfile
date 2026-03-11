# Change the base image to the latest SonarQube LTS version or any other preferred version
FROM sonarqube:26.3.0.120487-community 

USER root

    # Install JDK + tools 
    ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
    RUN apt-get update \
    && apt-get install -y --no-install-recommends openjdk-21-jdk-headless curl unzip git ca-certificates \
    && rm -rf /var/lib/apt/lists/*

USER sonarqube
ENV PATH="${JAVA_HOME}/bin:${PATH}"
ENV SONAR_JAVA_PATH="/usr/lib/jvm/java-21-openjdk-amd64/bin/java"