/**
 * LoggingService.js
 * 
 * A comprehensive logging service for ImmiMate application that:
 * 1. Controls logging based on environment (development/production)
 * 2. Provides different log levels (debug, info, warn, error)
 * 3. Formats logs with timestamps and level indicators
 * 4. Works with run_with_logs.sh to pipe logs to files
 */

/**
 * Logging service for consistent logging across the application.
 * Supports different log levels and can be configured to disable logs in production.
 */
class LoggingService {
    constructor() {
        this.LOG_LEVELS = {
            DEBUG: 0,
            INFO: 1,
            WARN: 2,
            ERROR: 3
        };
        
        // Set default log level based on environment
        this.currentLogLevel = process.env.NODE_ENV === 'production' 
            ? this.LOG_LEVELS.ERROR  // Only show errors in production
            : this.LOG_LEVELS.DEBUG; // Show all logs in development
    }
    
    /**
     * Set the current log level.
     * @param {string} level - The log level to set ('DEBUG', 'INFO', 'WARN', 'ERROR')
     */
    setLogLevel(level) {
        if (this.LOG_LEVELS[level] !== undefined) {
            this.currentLogLevel = this.LOG_LEVELS[level];
            this.info(`Log level set to ${level}`);
        } else {
            this.error(`Invalid log level: ${level}`);
        }
    }
    
    /**
     * Log a debug message.
     * @param {string} message - The message to log
     * @param {any} [data] - Optional data to log
     */
    debug(message, data) {
        if (this.currentLogLevel <= this.LOG_LEVELS.DEBUG) {
            if (data) {
                console.debug(`[DEBUG] ${message}`, data);
            } else {
                console.debug(`[DEBUG] ${message}`);
            }
        }
    }
    
    /**
     * Log an info message.
     * @param {string} message - The message to log
     * @param {any} [data] - Optional data to log
     */
    info(message, data) {
        if (this.currentLogLevel <= this.LOG_LEVELS.INFO) {
            if (data) {
                console.info(`[INFO] ${message}`, data);
            } else {
                console.info(`[INFO] ${message}`);
            }
        }
    }
    
    /**
     * Log a warning message.
     * @param {string} message - The message to log
     * @param {any} [data] - Optional data to log
     */
    warn(message, data) {
        if (this.currentLogLevel <= this.LOG_LEVELS.WARN) {
            if (data) {
                console.warn(`[WARN] ${message}`, data);
            } else {
                console.warn(`[WARN] ${message}`);
            }
        }
    }
    
    /**
     * Log an error message.
     * @param {string} message - The message to log
     * @param {any} [error] - Optional error to log
     */
    error(message, error) {
        if (this.currentLogLevel <= this.LOG_LEVELS.ERROR) {
            if (error) {
                console.error(`[ERROR] ${message}`, error);
            } else {
                console.error(`[ERROR] ${message}`);
            }
        }
    }
}

// Create a singleton instance
const Logger = new LoggingService();

export default Logger; 