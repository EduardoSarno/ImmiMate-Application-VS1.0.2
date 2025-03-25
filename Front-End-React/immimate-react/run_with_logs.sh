#!/bin/bash

# --------------------- Configuration ---------------------
# Maximum number of log files to keep
MAX_LOGS=30

# Log directory
LOG_DIR="logs"

# --------------------- Functions ---------------------
# Cleanup old logs if we exceed the maximum
cleanup_old_logs() {
  echo "Checking for old logs to clean up..."
  
  # Count existing log files
  local log_count=$(ls -1 "$LOG_DIR"/frontend_*.log 2>/dev/null | wc -l | tr -d ' ')
  
  # Remove oldest logs if we exceed the maximum
  if [ "$log_count" -gt "$MAX_LOGS" ]; then
    local excess=$((log_count - MAX_LOGS))
    echo "Removing $excess old log files..."
    ls -t "$LOG_DIR"/frontend_*.log | tail -n "$excess" | xargs rm
  fi
}

# Set up environment
setup_environment() {
  # Create logs directory if it doesn't exist
  mkdir -p "$LOG_DIR"
  
  # Clean up old logs
  cleanup_old_logs
  
  # Create new log file with timestamp
  LOG_FILE="$LOG_DIR/frontend_$(date +%Y%m%d_%H%M%S).log"
  echo "Starting React application with logs going to $LOG_FILE"
  
  # Add log header
  echo "===============================================" > "$LOG_FILE"
  echo "ImmiMate Frontend Log - Started $(date)" >> "$LOG_FILE"
  echo "Environment: ${NODE_ENV:-development}" >> "$LOG_FILE"
  echo "===============================================" >> "$LOG_FILE"
  echo "" >> "$LOG_FILE"
}

# Main entry point
main() {
  setup_environment
  
  echo "Starting application with logs..."
  
  # Run the application with logs going to both console and file
  # This is the simplest and most reliable approach
  npm start 2>&1 | tee -a "$LOG_FILE"
  
  exit_code=$?
  
  # Log the exit
  echo "" >> "$LOG_FILE"
  echo "===============================================" >> "$LOG_FILE"
  echo "ImmiMate Frontend - Ended $(date)" >> "$LOG_FILE"
  echo "Exit code: $exit_code" >> "$LOG_FILE"
  echo "===============================================" >> "$LOG_FILE"
  
  return $exit_code
}

# Execute main function
main 