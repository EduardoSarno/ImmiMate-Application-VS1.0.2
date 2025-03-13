import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import { useAuth } from '../contexts/AuthContext';
import { getGoogleOAuthURL, processOAuthCallback, clearOAuthParams } from '../config';

const LoginPage = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  
  const { login, currentUser } = useAuth();
  
  // Check URL for OAuth callback parameters - just like in the original HTML
  useEffect(() => {
    console.log("LoginPage: Checking for OAuth callback parameters");
    
    const params = new URLSearchParams(location.search);
    const { token, email, error } = processOAuthCallback(params);
    
    if (error) {
      console.error("LoginPage: OAuth error received:", error);
      setError(`Authentication failed: ${error}`);
      clearOAuthParams('/login');
      return;
    }
    
    if (token && email) {
      console.log("LoginPage: OAuth callback detected with token and email");
      
      // Store token and email in localStorage
      localStorage.setItem('access_token', token);
      localStorage.setItem('user_email', email);
      
      console.log("LoginPage: Saved OAuth credentials to localStorage");
      
      // Show success message
      setSuccessMessage('Login successful via Google! Redirecting...');
      
      // Clear URL parameters to prevent reprocessing on refresh
      clearOAuthParams('/login');
      
      // After a short delay, redirect to the form page
      setTimeout(() => {
        navigate('/form');
      }, 1500);
    }
  }, [location, navigate]);
  
  // Check if already logged in
  useEffect(() => {
    console.log("LoginPage: Checking if user is already logged in");
    console.log("LoginPage: currentUser =", currentUser);
    
    // Redirect if user is already authenticated
    if (currentUser) {
      console.log("LoginPage: User is logged in, redirecting to /form");
      navigate('/form');
    }
  }, [currentUser, navigate]);
  
  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    
    try {
      await login(email, password);
      
      setSuccessMessage('Successfully logged in!');
      
      // Redirect after a short delay
      setTimeout(() => {
        navigate('/form');
      }, 1500);
    } catch (error) {
      setError(error.message || 'Login failed. Please check your credentials.');
    } finally {
      setLoading(false);
    }
  };
  
  const handleGoogleLogin = () => {
    console.log("LoginPage: Initiating Google login");
    
    // Use the same function as signup page for consistency
    const oauth2Url = getGoogleOAuthURL('/login');
    
    console.log("LoginPage: OAuth URL:", oauth2Url);
    window.location.href = oauth2Url;
  };
  
  return (
    <>
      <Navbar />
      
      <div className="auth-container">
        <h2>Log In</h2>
        <p className="auth-subtext">Access your account and continue your immigration journey.</p>
        
        {/* Google Sign-in Button */}
        <div className="oauth-section">
          <p className="oauth-text">Quick sign-in with Google:</p>
          <button 
            type="button" 
            className="login-with-google-btn" 
            onClick={handleGoogleLogin}
          >
            <img 
              src="/Images/google_logo.png" 
              alt="Google logo" 
              className="google-icon" 
            />
            Sign in with Google
          </button>
          <div className="divider">
            <span>OR</span>
          </div>
        </div>
        
        {error && (
          <div className="field-error">{error}</div>
        )}
        
        {successMessage && (
          <div style={{ 
            display: 'block', 
            color: 'green', 
            backgroundColor: '#e8f5e9', 
            padding: '10px', 
            borderRadius: '5px', 
            marginBottom: '15px' 
          }}>
            {successMessage}
          </div>
        )}
        
        <form className="auth-form" onSubmit={handleSubmit}>
          <input 
            type="email" 
            placeholder="Email" 
            required 
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoComplete="username"
          />
          <input 
            type="password" 
            placeholder="Password" 
            required 
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
          />
          <button 
            type="submit" 
            className="btn-primary"
            disabled={loading}
          >
            {loading ? 'Logging in...' : 'Log In'}
          </button>
        </form>
        
        <p className="auth-switch">
          Don't have an account? <Link to="/signup">Sign Up</Link>
        </p>
      </div>
      
      <Footer />
    </>
  );
};

export default LoginPage; 