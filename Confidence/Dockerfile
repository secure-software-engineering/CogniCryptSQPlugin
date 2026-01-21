 # Use official Python image
FROM python:3.11-slim

# Set working directory
WORKDIR /app

# Copy requirements and install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy rest of the codebase
COPY . .

# Expose port for Flask
EXPOSE 8001

# Run the app
CMD ["python", "main.py"]