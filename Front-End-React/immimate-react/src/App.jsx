import React, { useEffect, useState } from 'react';
import { Routes, Route, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from './contexts/AuthContext';
import Logger from './utils/LoggingService';
import axios from 'axios';

// Pages
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import FormPage from './pages/FormPage';
import ChatPage from './pages/ChatPage';
import DashboardPage from './pages/DashboardPage';
import LoggingExample from './components/LoggingExample';

// Add styles for the OAuth loading spinner
const oauthLoadingStyles = `
.oauth-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background-color: #f8f9fa;
}

.loading-spinner {
  border: 6px solid #f3f3f3;
  border-top: 6px solid #3498db;
  border-radius: 50%;
  width: 50px;
  height: 50px;
  animation: spin 1.5s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.oauth-loading p {
  font-size: 18px;
  color: #333;
  font-family: Arial, sans-serif;
}
`;

// Inject the styles into the document
const style = document.createElement('style');
style.textContent = oauthLoadingStyles;
document.head.appendChild(style);

// Protected route component
const ProtectedRoute = ({ children }) => {
  const { currentUser, loading, verifyAuth } = useAuth();
  const navigate = useNavigate();
  const [isVerifying, setIsVerifying] = useState(false);
  const [waitingForAuth, setWaitingForAuth] = useState(false);
  
  // Check auth status once on mount
  useEffect(() => {
    const checkAuth = async () => {
      // Only verify if we're not already loading or have a user
      if (!loading && !currentUser && !isVerifying) {
        setIsVerifying(true);
        setWaitingForAuth(true);
        Logger.debug("ProtectedRoute: Verifying authentication status");
        
        try {
          const user = await verifyAuth();
          if (user) {
            Logger.debug("ProtectedRoute: Authentication verified successfully");
          } else {
            Logger.warn("ProtectedRoute: Authentication verification returned no user");
          }
        } catch (error) {
          Logger.error("ProtectedRoute: Authentication verification failed", error);
        } finally {
          setIsVerifying(false);
          setWaitingForAuth(false);
        }
      }
    };
    
    checkAuth();
  }, [verifyAuth, loading, currentUser, isVerifying]);
  
  // Handle redirection if auth fails
  useEffect(() => {
    // Check if we have a userId in localStorage that might indicate we should wait for verification
    // Note: localStorage is only used for user identification, authentication comes from HTTP-only cookies
    const hasUserIdInStorage = !!localStorage.getItem('user_id');
    
    if (!loading && !currentUser && hasUserIdInStorage && !waitingForAuth) {
      Logger.info("ProtectedRoute: User ID found in localStorage but not in state, waiting for verification");
      setWaitingForAuth(true);
      
      // Set a timeout to redirect if the verification doesn't set the currentUser
      const timer = setTimeout(() => {
        if (!currentUser) {
          Logger.warn("ProtectedRoute: Verification failed, redirecting to login");
          navigate('/login');
        }
      }, 2000);
      
      return () => {
        clearTimeout(timer);
      };
    }
  }, [currentUser, loading, navigate, waitingForAuth]);
  
  Logger.debug("ProtectedRoute: Checking authentication");
  Logger.debug("ProtectedRoute: currentUser =", currentUser);
  Logger.debug("ProtectedRoute: loading =", loading);
  
  // No longer checking for tokens in URL since we're using HTTP-only cookies
  Logger.debug("ProtectedRoute: token in URL check removed, using HTTP-only cookies");
  
  // If still loading or verifying, show loading indicator
  if (loading || isVerifying) {
    Logger.debug("ProtectedRoute: Still loading, showing loading indicator");
    return <div className="loading-container">
      <div className="loading-spinner"></div>
      <p>Loading your profile...</p>
    </div>;
  }
  
  // If authenticated in state, render children
  if (currentUser) {
    Logger.info("ProtectedRoute: User authenticated, allowing access");
    return children;
  }
  
  // If we have a stored user but no currentUser and we're waiting for auth, show loading
  if (waitingForAuth) {
    return <div className="loading-container">
      <div className="loading-spinner"></div>
      <p>Verifying your credentials...</p>
    </div>;
  }
  
  // If not authenticated, redirect to login
  Logger.warn("ProtectedRoute: No user found, redirecting to /login");
  return <Navigate to="/login" />;
};

function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/oauth/callback" element={<OAuthSuccessHandler />} />
      <Route path="/logging-example" element={<LoggingExample />} />
      <Route 
        path="/dashboard" 
        element={
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        } 
      />
      <Route 
        path="/form" 
        element={
          <ProtectedRoute>
            <FormPage />
          </ProtectedRoute>
        } 
      />
      <Route 
        path="/chat" 
        element={
          <ProtectedRoute>
            <ChatPage />
          </ProtectedRoute>
        } 
      />
    </Routes>
  );
}

// Component to handle OAuth callback
const OAuthSuccessHandler = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { verifyAuth, setCurrentUser } = useAuth();
  const [loading, setLoading] = useState(true);
  
  // Function to check if user has a profile
  const checkUserProfile = async () => {
    try {
      const API_BASE_URL = 'http://localhost:8080/api';
      // Make a request to the profiles/recent endpoint
      const response = await axios.get(`${API_BASE_URL}/profiles/recent`, {
        withCredentials: true // Important for JWT cookie
      });
      
      Logger.debug('Profile check response:', response.status, response.data);
      
      // If the response indicates a profile exists, return true
      return response.data && response.data.profileExists === true;
    } catch (error) {
      Logger.error('Error checking user profile:', error);
      // If there's an error, assume no profile
      return false;
    }
  };
  
  useEffect(() => {
    const handleOAuthCallback = async () => {
      setLoading(true);
      Logger.info("OAuthSuccessHandler: Processing OAuth callback");
      Logger.debug("OAuthSuccessHandler: Full URL search:", location.search);
      const params = new URLSearchParams(location.search);
      const email = params.get('email');
      const userId = params.get('userId');
      const isNewUser = params.get('newUser') === 'true';
      
      Logger.debug("OAuthSuccessHandler: Email:", email);
      Logger.debug("OAuthSuccessHandler: UserId:", userId);
      Logger.debug("OAuthSuccessHandler: New User:", isNewUser);
      
      if (email && userId) {
        // Store user identification info in localStorage
        // (but not authentication tokens as they're in HTTP-only cookies now)
        localStorage.setItem('user_email', email);
        localStorage.setItem('user_id', userId);
        
        // If this is a new user, redirect directly to the form page
        if (isNewUser) {
          Logger.info("OAuthSuccessHandler: New user detected, redirecting to form");
          navigate('/form', { replace: true });
          setLoading(false);
          return;
        }
        
        // Verify authentication with server (this will use the JWT cookie)
        try {
          Logger.debug("OAuthSuccessHandler: Verifying authentication with server");
          const userData = await verifyAuth();
          
          if (userData) {
            Logger.info("OAuthSuccessHandler: Authentication verified successfully");
            
            // Check if user has a profile
            const hasProfile = await checkUserProfile();
            Logger.info(`OAuthSuccessHandler: User has profile? ${hasProfile}`);
            
            // Redirect based on whether they have a profile
            if (hasProfile) {
              Logger.info("OAuthSuccessHandler: User has a profile, redirecting to dashboard");
              navigate('/dashboard', { replace: true });
            } else {
              Logger.info("OAuthSuccessHandler: User doesn't have a profile, redirecting to form");
              navigate('/form', { replace: true });
            }
          } else {
            // If verifyAuth doesn't throw but returns no user, create a fallback user object
            Logger.warn("OAuthSuccessHandler: Auth verification returned no user data, using fallback");
            const fallbackUser = {
              email: email,
              id: userId,
              // Add other necessary fields with default values
              role: 'USER',
              firstName: email.split('@')[0] || 'User',
              lastName: ''
            };
            
            // Set the user directly in AuthContext
            setCurrentUser(fallbackUser);
            
            // Check if user has a profile
            const hasProfile = await checkUserProfile();
            Logger.info(`OAuthSuccessHandler: User has profile? ${hasProfile}`);
            
            // Redirect based on whether they have a profile
            if (hasProfile) {
              Logger.info("OAuthSuccessHandler: User has a profile, redirecting to dashboard");
              navigate('/dashboard', { replace: true });
            } else {
              Logger.info("OAuthSuccessHandler: User doesn't have a profile, redirecting to form");
              navigate('/form', { replace: true });
            }
          }
        } catch (error) {
          // If verification completely fails, still try to set a user to avoid login loop
          Logger.error("OAuthSuccessHandler: Authentication verification failed:", error);
          
          // Create a fallback user based on the data we have
          const fallbackUser = {
            email: email,
            id: userId,
            // Add other necessary fields with default values
            role: 'USER',
            firstName: email.split('@')[0] || 'User',
            lastName: ''
          };
          
          // Try to set the user directly
          setCurrentUser(fallbackUser);
          
          // In case of auth error, redirect to form (safer default)
          Logger.warn("OAuthSuccessHandler: Using emergency fallback user data, redirecting to form");
          navigate('/form', { replace: true });
        }
      } else {
        Logger.error("OAuthSuccessHandler: Missing email or userId, redirecting to login");
        navigate('/login', { replace: true });
      }
      
      setLoading(false);
    };
    
    handleOAuthCallback();
  }, [location, navigate, verifyAuth, setCurrentUser]);
  
  if (loading) {
    return (
      <div className="oauth-loading">
        <div className="loading-spinner"></div>
        <p>Processing authentication, please wait...</p>
      </div>
    );
  }
  
  return null;
};

export default App; 