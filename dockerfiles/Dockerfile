# Chnage the base image to the latest SonarQube LTS version or any other preferred version
FROM sonarqube:25.2.0.102705-community 

USER root

    # Install JDK + tools 
    ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
    RUN apt-get update \
    && apt-get install -y --no-install-recommends openjdk-21-jdk-headless curl unzip git ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Install Maven
ENV MAVEN_VERSION=3.9.11
ENV MAVEN_HOME=/opt/maven
RUN apt-get update && \
    apt-get install -y curl unzip git && \
    curl -fsSL https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz | \
    tar -xz -C /opt && \
    ln -s /opt/apache-maven-${MAVEN_VERSION} ${MAVEN_HOME} && \
    ln -s ${MAVEN_HOME}/bin/mvn /usr/bin/mvn

# Install Gradle
ENV GRADLE_VERSION=8.5
ENV GRADLE_HOME=/opt/gradle
RUN curl -fsSL https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -o gradle.zip && \
    unzip gradle.zip -d /opt && \
    rm gradle.zip && \
    ln -s /opt/gradle-${GRADLE_VERSION} ${GRADLE_HOME} && \
    ln -s ${GRADLE_HOME}/bin/gradle /usr/bin/gradle



USER sonarqube
ENV PATH="${JAVA_HOME}/bin:${MAVEN_HOME}/bin:${GRADLE_HOME}/bin:${PATH}"
ENV SONAR_JAVA_PATH = "/usr/lib/jvm/java-21-openjdk-amd64/bin/java"