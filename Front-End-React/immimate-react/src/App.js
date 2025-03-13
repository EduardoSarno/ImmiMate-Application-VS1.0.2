import React, { useEffect } from 'react';
import { Routes, Route, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from './contexts/AuthContext';

// Pages
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import FormPage from './pages/FormPage';
import ChatPage from './pages/ChatPage';
import DashboardPage from './pages/DashboardPage';

// Protected route component
const ProtectedRoute = ({ children }) => {
  const { currentUser, loading } = useAuth();
  const location = useLocation();
  
  console.log("ProtectedRoute: Checking authentication");
  console.log("ProtectedRoute: currentUser =", currentUser);
  console.log("ProtectedRoute: loading =", loading);
  
  // Check if the URL contains a token (coming from OAuth redirect)
  const hasTokenInUrl = location.search.includes('token=');
  console.log("ProtectedRoute: token in URL =", hasTokenInUrl ? "Yes" : "No");
  
  // If we have a token in the URL, allow access to process it
  if (hasTokenInUrl) {
    console.log("ProtectedRoute: Token found in URL, allowing access");
    return children;
  }
  
  // If still loading, show loading indicator
  if (loading) {
    console.log("ProtectedRoute: Still loading, showing loading indicator");
    return <div>Loading...</div>;
  }
  
  // If authenticated, render children
  if (currentUser) {
    console.log("ProtectedRoute: User authenticated, allowing access");
    return children;
  }
  
  // If not authenticated, redirect to login
  console.log("ProtectedRoute: No user found, redirecting to /login");
  return <Navigate to="/login" />;
};

function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/oauth/callback" element={<OAuthSuccessHandler />} />
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
  const { setCurrentUser } = useAuth();
  
  useEffect(() => {
    const handleOAuthCallback = async () => {
      console.log("OAuthSuccessHandler: Processing OAuth callback");
      console.log("OAuthSuccessHandler: Full URL search:", location.search);
      const params = new URLSearchParams(location.search);
      const token = params.get('token');
      const email = params.get('email');
      const userId = params.get('userId');
      
      console.log("OAuthSuccessHandler: Token present:", token ? `Yes (length: ${token.length})` : "No");
      console.log("OAuthSuccessHandler: Email:", email);
      console.log("OAuthSuccessHandler: UserId:", userId);
      
      if (token && email) {
        // Store credentials
        localStorage.setItem('access_token', token);
        localStorage.setItem('user_email', email);
        if (userId) localStorage.setItem('user_id', userId);
        
        console.log("OAuthSuccessHandler: Stored credentials in localStorage");
        
        // Set current user in AuthContext
        setCurrentUser({
          email: email,
          id: userId
        });
        
        console.log("OAuthSuccessHandler: Set current user in AuthContext");
        console.log("OAuthSuccessHandler: Redirecting to dashboard");
        
        // Redirect to dashboard
        navigate('/dashboard', { replace: true });
      } else {
        console.log("OAuthSuccessHandler: Missing token or email, redirecting to login");
        navigate('/login', { replace: true });
      }
    };
    
    handleOAuthCallback();
  }, [location, navigate, setCurrentUser]);
  
  return <div>Processing authentication, please wait...</div>;
};

export default App; 