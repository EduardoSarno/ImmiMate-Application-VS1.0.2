import React, { useContext, useState, useEffect, createContext } from 'react';

// Base URL for API requests
const API_BASE_URL = 'http://localhost:8080/api';

export const AuthContext = createContext();

export function useAuth() {
  return useContext(AuthContext);
}

export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Verify token with backend
  const verifyToken = async (token) => {
    try {
      if (!token) {
        console.error('AuthContext: Cannot verify null or empty token');
        return null;
      }
      
      console.log(`AuthContext: Verifying token with backend, token length: ${token.length}`);
      console.log(`AuthContext: First 10 chars of token: ${token.substring(0, 10)}...`);
      
      const response = await fetch(`${API_BASE_URL}/auth/me`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      console.log(`AuthContext: Token verification response status: ${response.status}`);
      
      if (response.ok) {
        const userData = await response.json();
        console.log('AuthContext: Token verified successfully, user data:', userData);
        return userData;
      } else {
        console.error('AuthContext: Token verification failed, status:', response.status);
        const errorText = await response.text();
        console.error('AuthContext: Error response:', errorText);
        return null;
      }
    } catch (error) {
      console.error('AuthContext: Error verifying token:', error);
      return null;
    }
  };

  // Load user from localStorage on initial render
  useEffect(() => {
    async function validateToken() {
      try {
        console.log('AuthContext: Initial load - checking for stored token');
        const token = localStorage.getItem('access_token');
        
        if (!token) {
          console.log('AuthContext: No stored token found');
          setCurrentUser(null);
          setLoading(false);
          return;
        }
        
        console.log(`AuthContext: Found stored token (length: ${token.length}), verifying...`);
        console.log(`AuthContext: First 10 chars of token: ${token.substring(0, 10)}...`);
        
        // Verify the token with the backend
        const userData = await verifyToken(token);
        
        if (userData) {
          console.log('AuthContext: Valid token, restoring user session');
          
          // Restore user session
          setCurrentUser({
            email: userData.email,
            id: userData.id,
            role: userData.role,
            name: userData.name,
            firstName: userData.firstName,
            lastName: userData.lastName
          });
        } else {
          console.warn('AuthContext: Invalid token, clearing localStorage');
          localStorage.removeItem('access_token');
          localStorage.removeItem('user_email');
          localStorage.removeItem('user_id');
          setCurrentUser(null);
        }
      } catch (error) {
        console.error('AuthContext: Error during authentication check:', error);
        setCurrentUser(null);
      } finally {
        setLoading(false);
      }
    }

    validateToken();
  }, []);

  const login = async (email, password) => {
    try {
      const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ email, password })
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Login failed');
      }

      const data = await response.json();
      
      // Save token and user info to localStorage
      localStorage.setItem('access_token', data.token);
      localStorage.setItem('user_email', email);
      localStorage.setItem('user_id', data.userId);
      if (data.role) localStorage.setItem('user_role', data.role);
      
      console.log('AuthContext: Login successful, setting current user');
      
      // Set current user state
      setCurrentUser({
        email: email,
        id: data.userId,
        role: data.role
      });
      
      return {
        success: true,
        user: {
          email: email,
          id: data.userId
        }
      };
    } catch (error) {
      console.error('AuthContext: Login error:', error);
      return {
        success: false,
        error: error.message
      };
    }
  };

  const logout = () => {
    // Clear user data from localStorage
    localStorage.removeItem('access_token');
    localStorage.removeItem('user_email');
    localStorage.removeItem('user_id');
    localStorage.removeItem('user_role');
    
    console.log('AuthContext: User logged out, session cleared');
    
    // Clear current user state
    setCurrentUser(null);
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
      
      console.log('AuthContext: Attempting to register user:', email);
      
      const response = await fetch(`${API_BASE_URL}/auth/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestBody)
      });

      const data = await response.json();
      
      if (!response.ok) {
        console.error('AuthContext: Registration failed:', data);
        throw new Error(data.message || 'Registration failed');
      }

      console.log('AuthContext: Registration successful for:', email);
      
      return {
        success: true,
        message: data.message || 'Registration successful'
      };
    } catch (error) {
      console.error('AuthContext: Signup error:', error);
      return {
        success: false,
        error: error.message || 'Registration failed'
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
    verifyToken
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
} 