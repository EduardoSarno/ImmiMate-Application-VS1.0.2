import React, { useEffect } from 'react';
import Logger from './LoggingService';

/**
 * Example component demonstrating the use of LoggingService
 * This is not meant to be used in production, just as a reference
 */
const LoggingExample = () => {
  useEffect(() => {
    // Show examples of different log levels
    demonstrateLogging();
  }, []);
  
  const demonstrateLogging = () => {
    // Debug level - only shown in development
    Logger.debug('This is a debug message');
    Logger.debug('Debug with data:', { user: 'John', action: 'login' });
    
    // Info level - only shown in development by default
    Logger.info('This is an info message');
    Logger.info('Info with data:', { status: 'success', code: 200 });
    
    // Warning level - shown in all environments
    Logger.warn('This is a warning message');
    Logger.warn('Warning with data:', { problem: 'disk space low', remaining: '10%' });
    
    // Error level - shown in all environments
    Logger.error('This is an error message');
    Logger.error('Error with data:', new Error('Something went wrong'));
    
    // Critical - highest level, shown in all environments and could be sent to server
    Logger.critical('This is a critical error!', { 
      component: 'PaymentProcessor', 
      error: 'Database connection failed' 
    });
  };
  
  return (
    <div className="logging-example">
      <h2>Logging Service Example</h2>
      <p>
        Open your browser console to see the logged messages.
        Different log levels will be visible depending on your environment.
      </p>
      <button onClick={demonstrateLogging}>
        Log Example Messages
      </button>
      <div className="log-level-info">
        <h3>Log Levels:</h3>
        <ul>
          <li><strong>DEBUG</strong> - Detailed information (dev only)</li>
          <li><strong>INFO</strong> - General information (dev only by default)</li>
          <li><strong>WARN</strong> - Warning situations (all environments)</li>
          <li><strong>ERROR</strong> - Error conditions (all environments)</li>
          <li><strong>CRITICAL</strong> - Critical failures (all environments, may be sent to server)</li>
        </ul>
      </div>
      
      <div className="example-code">
        <h3>Example Usage:</h3>
        <pre>
{`import Logger from '../utils/LoggingService';

// In your component:
Logger.info('User logged in:', userData);
Logger.error('Failed to load data:', error);`}
        </pre>
      </div>
    </div>
  );
};

export default LoggingExample; 