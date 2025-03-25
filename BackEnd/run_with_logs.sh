#!/bin/bash

# Create logs directory if it doesn't exist
mkdir -p logs

# Set log file with timestamp
LOG_FILE="logs/app_$(date +%Y%m%d_%H%M%S).log"
echo "Starting application with logs going to $LOG_FILE"

# Load environment variables from .env file
echo "Loading environment variables from .env file"
export $(grep -v '^#' .env | xargs)

# Run the application with logs going to both console and file (using tee)
mvn spring-boot:run | tee -a "$LOG_FILE" 