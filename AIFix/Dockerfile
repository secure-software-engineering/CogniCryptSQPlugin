# Use official Python image
FROM python:3.10-slim

# Install JDK + tools
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
RUN apt-get update \
&& apt-get install -y --no-install-recommends openjdk-21-jdk-headless curl unzip git ca-certificates \
&& rm -rf /var/lib/apt/lists/*

# Set JAVA_HOME and PATH (works on Debian-based python:slim)
ENV JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
ENV PATH="$JAVA_HOME/bin:${PATH}"

# Set working directory
WORKDIR /app

# Copy requirements and install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy rest of the codebase
COPY . .

# Expose port for Flask
EXPOSE 5000

# Run the app
CMD ["python", "main.py"]
