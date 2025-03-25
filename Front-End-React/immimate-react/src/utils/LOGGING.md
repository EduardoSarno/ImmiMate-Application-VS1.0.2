# ImmiMate Application Logging System

This document outlines the logging system implemented in the ImmiMate application.

## Overview

The logging system provides a consistent way to log messages across the application, with different severity levels and formats. It supports:

- Different log levels (debug, info, warn, error)
- Environment-aware logging (development vs. production)
- Timestamps on all logs
- Structured logging with support for objects and Error instances
- Integration with both the front-end console and a server-side logging mechanism

## Log Levels

The system supports four log levels:

1. **DEBUG** - Detailed information, primarily for developers, used for diagnosing problems.
2. **INFO** - General information about system operation.
3. **WARN** - Warnings that indicate potential issues that aren't critical.
4. **ERROR** - Error conditions that need to be addressed but don't necessarily cause the application to fail.

## Using the Logger Service

### Basic Usage

```javascript
import Logger from '../utils/LoggingService';

// Different log levels
Logger.debug('Debug message');
Logger.info('Information message');
Logger.warn('Warning message');
Logger.error('Error message');
```

### Logging Objects

```javascript
const user = { id: 123, name: 'John Doe' };
Logger.info('User details:', user);
```

### Logging Errors

```javascript
try {
  // Code that might throw
  throw new Error('Something went wrong');
} catch (error) {
  Logger.error('Operation failed:', error);
}
```

### Using Context Prefixes

For related logs, use a consistent prefix:

```javascript
Logger.debug('[AUTH] Checking authentication status');
Logger.info('[AUTH] User authenticated successfully');
```

## Log File Management

### Server-Side Logs

The application uses a Node.js script to capture and persist logs. This script:

1. Captures console output from the application
2. Adds timestamps to log entries
3. Saves logs to files in the `logs` directory
4. Implements log rotation to manage file size and retention

### Running with Logs

To run the application with logging enabled:

```bash
./run_with_logs.sh
```

This script will:
- Start the application
- Capture console output
- Save logs to timestamped files in the `logs` directory
- Maintain a maximum of 30 log files (configurable)

## Best Practices

1. **Use appropriate log levels**:
   - Use `debug` for detailed information only needed during development
   - Use `info` for general operational information
   - Use `warn` for potential issues
   - Use `error` for actual errors

2. **Be concise but informative**:
   - Include relevant context in log messages
   - For components, include the component name
   - For API calls, include endpoints and response codes

3. **Handle sensitive data properly**:
   - Never log passwords, tokens, or personal data
   - Log the presence of data, not the data itself (e.g., "User email provided: true")

4. **Use structured logging where possible**:
   - Log objects rather than concatenating strings
   - This makes logs easier to parse and analyze

5. **Include a context identifier**:
   - Use prefixes like `[AUTH]`, `[API]`, `[FORM]` for related logs
   - This makes it easier to follow the flow in log files

## Example Component

See `src/components/LoggingExample.jsx` for a comprehensive example of using the Logger service in a React component.

## Configuration

Logging behavior can be configured through environment variables:

- `REACT_APP_LOG_LEVEL`: Set minimum log level (debug, info, warn, error)
- `REACT_APP_DISABLE_LOGS`: Set to 'true' to disable all logging

## Troubleshooting

If logs are not appearing:

1. Check that the Logger service is properly imported
2. Verify that the log level is appropriate (debug logs won't show if log level is set to info)
3. Ensure the run_with_logs.sh script has executable permissions
4. Check that the logs directory exists and is writable 