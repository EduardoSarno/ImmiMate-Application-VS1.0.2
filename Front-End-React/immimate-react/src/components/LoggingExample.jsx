import React, { useState, useEffect } from 'react';
import Logger from '../utils/LoggingService';

/**
 * Example component demonstrating proper usage of the Logger service
 * This can be used as a reference for updating other components in the application
 */
const LoggingExample = () => {
  const [count, setCount] = useState(0);

  useEffect(() => {
    // Different log levels with examples
    Logger.debug('This is a debug message - detailed information for debugging');
    Logger.info('This is an info message - general information about system operation');
    Logger.warn('This is a warning message - potential issue that might need attention');
    Logger.error('This is an error message - something went wrong but execution continues');
    
    // Logging with multiple arguments
    const user = { id: 123, name: 'John Doe' };
    Logger.debug('User details:', user);
    
    // Logging with object inspection
    Logger.debug('Component state:', { count });
    
    // Error logging with Error objects
    try {
      throw new Error('Example error');
    } catch (error) {
      Logger.error('Caught an error:', error);
      Logger.error('Error stack:', error.stack);
    }
    
    // Conditional logging
    if (count > 5) {
      Logger.warn('Count is getting high:', count);
    }
    
    // Logging performance measurements
    const start = performance.now();
    // Some operation
    const end = performance.now();
    Logger.debug(`Operation took ${end - start}ms to complete`);
    
    // Using context prefix for related logs
    Logger.debug('[AUTH] Checking authentication status');
    Logger.info('[AUTH] User successfully authenticated');
    
    // Logging API responses
    const mockApiResponse = { status: 200, data: { success: true } };
    Logger.debug('API response:', mockApiResponse);
    
    // Logging important business events
    Logger.info('[BUSINESS] User completed profile form');
    
    // Example of how to handle sensitive data
    const sensitiveData = { email: 'user@example.com', password: 'password123' };
    // Don't log sensitive data directly
    Logger.debug('User logged in:', { email: sensitiveData.email, hasPassword: !!sensitiveData.password });
    
    return () => {
      Logger.info('Component unmounting');
    };
  }, [count]);

  const handleClick = () => {
    Logger.debug('Button clicked, incrementing count');
    setCount(prevCount => prevCount + 1);
    Logger.debug('Count incremented to:', count + 1);
  };

  const handleError = () => {
    Logger.debug('Error button clicked');
    try {
      // Simulate an error
      throw new Error('User triggered error');
    } catch (error) {
      Logger.error('Error in button handler:', error.message);
    }
  };

  return (
    <div className="logging-example">
      <h2>Logger Service Example</h2>
      <p>Open the browser console to see log messages</p>
      <p>Count: {count}</p>
      <button onClick={handleClick}>Increment Count (generates logs)</button>
      <button onClick={handleError}>Trigger Error (generates error log)</button>
      
      <div className="documentation">
        <h3>Logger Usage Examples</h3>
        <pre>
          {`
// Import the Logger service
import Logger from '../utils/LoggingService';

// Different log levels
Logger.debug('Debug information');
Logger.info('Informational message');
Logger.warn('Warning message');
Logger.error('Error message');

// Logging with multiple arguments
Logger.debug('User details:', user);

// Logging errors
try {
  // Something that might throw
} catch (error) {
  Logger.error('Operation failed:', error);
}
          `}
        </pre>
      </div>
    </div>
  );
};

export default LoggingExample; 