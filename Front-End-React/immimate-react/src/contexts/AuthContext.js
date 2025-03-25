import React, { useContext, useState, useEffect, createContext, useCallback } from 'react';
import Logger from '../utils/LoggingService';
import axios from 'axios';

// Base URL for API requests
const API_BASE_URL = 'http://localhost:8080/api';

// Minimum time (in ms) between auth verification attempts to prevent spamming the server
const AUTH_CHECK_INTERVAL = 60000; // 1 minute

export const AuthContext = createContext();

export function useAuth() {
  return useContext(AuthContext);
}

export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Keep track of the last authentication check to prevent loops
  const [lastAuthCheck, setLastAuthCheck] = useState(null);

  // Helper function to clear all auth data - wrapped in useCallback to prevent recreation on every render
  const clearAuthData = useCallback(() => {
    localStorage.removeItem('access_token');
    localStorage.removeItem('user_email');
    localStorage.removeItem('user_id');
    localStorage.removeItem('user_role');
    setCurrentUser(null);
    setError('Authentication failed');
  }, []);  // Empty dependency array since it only uses setState functions which are stable

  const verifyAuth = useCallback(async () => {
    // Throttle verification attempts - only check once per minute at most
    const now = Date.now();
    if (now - lastAuthCheck < AUTH_CHECK_INTERVAL) {
      return currentUser;
    }
    
    setLastAuthCheck(now);
    
    try {
      setLoading(true);
      Logger.debug('AuthContext: Verifying authentication with server');
      
      const response = await axios.get(`${API_BASE_URL}/auth/verify`, {
        withCredentials: true // Important for cookies
      });
      
      // Handle server response
      if (response.data && response.data.success) {
        // The backend returns user data in the 'data' field, not 'user'
        const userData = response.data.data;
        Logger.debug('AuthContext: Auth verified successfully:', userData);
        
        if (!userData) {
          Logger.warn('AuthContext: No user data in successful response');
          clearAuthData();
          setError('Authentication failed: Server returned success but no user data');
          return null;
        }
        
        // Ensure we have the user's email and ID in localStorage for future reference
        if (userData.email) localStorage.setItem('user_email', userData.email);
        if (userData.id) localStorage.setItem('user_id', userData.id);
        if (userData.role) localStorage.setItem('user_role', userData.role);
        
        // Update current user with the data from the server
        setCurrentUser(userData);
        setError(null);
        
        return userData;
      } else {
        const reason = response.data?.message || 'Unknown reason';
        Logger.debug(`AuthContext: Auth verification failed: ${reason}`, response.data);
        clearAuthData();
        setError(`Authentication failed: ${reason}`);
        return null;
      }
    } catch (err) {
      // More specific error messages based on error type
      let errorMessage = 'Authentication failed. Please try logging in again.';
      
      if (err.response) {
        // Server responded with an error status code
        if (err.response.status === 401) {
          errorMessage = 'Your session has expired. Please log in again.';
        } else if (err.response.status === 403) {
          errorMessage = 'You do not have permission to access this resource.';
        } else if (err.response.status >= 500) {
          errorMessage = 'Server error. Please try again later.';
        }
        // Include the server's error message if available
        if (err.response.data && err.response.data.message) {
          errorMessage += ` Details: ${err.response.data.message}`;
        }
      } else if (err.request) {
        // Request was made but no response was received
        errorMessage = 'No response from server. Please check your internet connection.';
      }
      
      Logger.error(`AuthContext: Error verifying authentication: ${errorMessage}`, err);
      clearAuthData();
      setError(errorMessage);
      return null;
    } finally {
      setLoading(false);
    }
  }, [lastAuthCheck, currentUser, clearAuthData]);
  
  // Load user from localStorage on initial render
  useEffect(() => {
    async function validateToken() {
      try {
        Logger.debug('AuthContext: Initial load - checking auth status');
        
        // Skip validation if we're already loading or have a current user
        if (loading || currentUser) {
          Logger.debug('AuthContext: Already loading or have current user, skipping validation');
          return;
        }
        
        // Check if we have a user ID in localStorage which indicates a potential valid session
        const userId = localStorage.getItem('user_id');
        const userEmail = localStorage.getItem('user_email');
        
        if (!userId || !userEmail) {
          Logger.debug('AuthContext: No stored user info found, clearing auth state');
          setCurrentUser(null);
          setLoading(false);
          return;
        }
        
        Logger.debug(`AuthContext: Found stored user (${userEmail}), verifying with server...`);
        
        // Always use the HTTP-only cookie based authentication
        const userData = await verifyAuth();
        
        if (userData) {
          Logger.info('AuthContext: Valid session, user authenticated');
          // User was set in verifyAuth(), nothing more to do
        } else {
          Logger.warn('AuthContext: Invalid session, clearing localStorage');
          localStorage.removeItem('access_token');
          localStorage.removeItem('user_email');
          localStorage.removeItem('user_id');
          localStorage.removeItem('user_role');
          setCurrentUser(null);
        }
      } catch (error) {
        Logger.error('AuthContext: Error during authentication check:', error);
        setCurrentUser(null);
      } finally {
        // Ensure loading is set to false even if there's an error
        setLoading(false);
      }
    }

    validateToken();
  }, [verifyAuth, loading, currentUser]);

  const login = async (email, password) => {
    try {
      setLoading(true);
      setError(null);
      
      Logger.debug('AuthContext: Attempting login for:', email);
      
      // Clear existing auth data before attempting a new login
      clearAuthData();
      
      const response = await axios.post(`${API_BASE_URL}/auth/login`, {
        email: email,
        password: password
      }, {
        withCredentials: true // Important for cookies
      });
      
      Logger.debug('AuthContext: Login response received:', response.data);
      
      if (!response.data || !response.data.success) {
        const reason = response.data?.message || 'Invalid credentials';
        Logger.warn(`AuthContext: Login failed: ${reason}`, response.data);
        setError(reason);
        return { success: false, message: reason };
      }
      
      // Extract data from response
      const data = response.data;
      
      // Store in localStorage for convenience
      localStorage.setItem('user_email', email);
      if (data.userId) localStorage.setItem('user_id', data.userId);
      if (data.role) localStorage.setItem('user_role', data.role);
      
      // Update application state
      const userObj = {
        email: email,
        id: data.userId || '',
        role: data.role || 'user'
      };
      setCurrentUser(userObj);
      
      try {
        // Immediately verify authentication to ensure JWT cookie is working
        const verifiedUser = await verifyAuth();
        
        // If verification doesn't return a user but we didn't hit an exception,
        // use the original user object
        if (!verifiedUser) {
          Logger.warn('AuthContext: Post-login verification did not return user data');
        }
        
        return {
          success: true,
          user: verifiedUser || userObj
        };
      } catch (verifyError) {
        Logger.error('AuthContext: Error during post-login verification:', verifyError);
        // Even if verification failed, we'll still consider login successful
        // since we got a successful response from the login endpoint
        return {
          success: true,
          user: userObj,
          warning: 'Logged in, but session verification failed.'
        };
      }
    } catch (error) {
      // Provide more specific error messages based on the error
      let errorMessage = 'Login failed. Please try again.';
      
      if (error.response) {
        // Server responded with an error status
        if (error.response.status === 401) {
          errorMessage = 'Invalid email or password.';
        } else if (error.response.status === 403) {
          errorMessage = 'Account is locked. Please contact support.';
        } else if (error.response.status === 429) {
          errorMessage = 'Too many login attempts. Please try again later.';
        } else if (error.response.status >= 500) {
          errorMessage = 'Server error. Please try again later.';
        }
        
        // Include the server's error message if available
        if (error.response.data && error.response.data.message) {
          errorMessage = error.response.data.message;
        }
      } else if (error.request) {
        // Request was made but no response was received
        errorMessage = 'No response from server. Please check your internet connection.';
      }
      
      Logger.error(`AuthContext: Login error: ${errorMessage}`, error);
      setError(errorMessage);
      
      return {
        success: false,
        message: errorMessage
      };
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    try {
      Logger.info('AuthContext: Logging out user');
      
      // Call the logout endpoint to clear HTTP-only cookies
      await axios.post(`${API_BASE_URL}/auth/logout`, {}, { 
        withCredentials: true 
      });
      
      // Clear user data from localStorage
      localStorage.removeItem('access_token');
      localStorage.removeItem('user_email');
      localStorage.removeItem('user_id');
      localStorage.removeItem('user_role');
      
      Logger.info('AuthContext: User logged out, session cleared');
      
      // Clear current user state
      setCurrentUser(null);
      
      return true;
    } catch (err) {
      Logger.error('AuthContext: Error during logout:', err);
      
      // Even if the server call fails, clear local state
      localStorage.removeItem('access_token');
      localStorage.removeItem('user_email');
      localStorage.removeItem('user_id');
      localStorage.removeItem('user_role');
      setCurrentUser(null);
      
      return false;
    }
  };

  const signup = async (email, password, phoneNumber, firstName, lastName) => {
    try {
      // Create request body with required fields
      const requestBody = { 
        email, 
        password,
        firstName,
        lastName
      };
      
      // Only add phoneNumber if it's provided
      if (phoneNumber) {
        requestBody.phoneNumber = phoneNumber;
      }
      
      Logger.info('AuthContext: Attempting to register user:', email);
      
      // Use axios with withCredentials
      const response = await axios.post(`${API_BASE_URL}/auth/register`,
        requestBody,
        {
          withCredentials: true,
          headers: {
            'Content-Type': 'application/json'
          }
        }
      );
      
      Logger.info('AuthContext: Registration successful for:', email);
      
      return {
        success: true,
        message: response.data.message || 'Registration successful'
      };
    } catch (error) {
      Logger.error('AuthContext: Signup error:', error);
      return {
        success: false,
        error: error.response?.data?.message || error.message || 'Registration failed'
      };
    }
  };

  // Context value
  const value = {
    currentUser,
    setCurrentUser,
    loading,
    login,
    logout,
    signup,
    verifyAuth,
    error
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
} 