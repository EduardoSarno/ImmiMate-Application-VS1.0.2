import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import { useAuth } from '../contexts/AuthContext';
import { getGoogleOAuthURL, processOAuthCallback, clearOAuthParams } from '../config';

const SignupPage = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  
  const { signup, currentUser, login } = useAuth();
  
  // Check URL for OAuth callback parameters - just like in the original HTML
  useEffect(() => {
    console.log("SignupPage: Checking for OAuth callback parameters");
    
    const params = new URLSearchParams(location.search);
    const { token, email, error } = processOAuthCallback(params);
    
    if (error) {
      console.error("SignupPage: OAuth error received:", error);
      setError(`Registration failed: ${error}`);
      clearOAuthParams('/signup');
      return;
    }
    
    if (token && email) {
      console.log("SignupPage: OAuth callback detected with token and email");
      
      // Store token and email in localStorage
      localStorage.setItem('access_token', token);
      localStorage.setItem('user_email', email);
      
      console.log("SignupPage: Saved OAuth credentials to localStorage");
      
      // Show success message
      setSuccessMessage('Registration successful via Google! Redirecting...');
      
      // Clear URL parameters to prevent reprocessing on refresh
      clearOAuthParams('/signup');
      
      // After a short delay, redirect to the form page
      setTimeout(() => {
        navigate('/form');
      }, 1500);
    }
  }, [location, navigate]);
  
  // Check if already logged in
  useEffect(() => {
    console.log("SignupPage: Checking if user is already logged in");
    console.log("SignupPage: currentUser =", currentUser);
    
    // Redirect if user is already authenticated
    if (currentUser) {
      console.log("SignupPage: User is logged in, redirecting to /form");
      navigate('/form');
    }
  }, [currentUser, navigate]);
  
  const validateForm = () => {
    setError('');
    
    // Validate passwords match
    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return false;
    }
    
    // Validate password strength
    if (password.length < 8) {
      setError('Password must be at least 8 characters');
      return false;
    }
    
    // Validate first name and last name are provided
    if (!firstName.trim()) {
      setError('First name is required');
      return false;
    }
    
    if (!lastName.trim()) {
      setError('Last name is required');
      return false;
    }
    
    return true;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }
    
    try {
      setError('');
      setLoading(true);
      
      // Create the new user with firstName and lastName (no phoneNumber)
      const signupResult = await signup(email, password, null, firstName, lastName);
      
      if (!signupResult.success) {
        throw new Error(signupResult.error || 'Registration failed');
      }
      
      setSuccessMessage('Account created successfully! Logging you in...');
      
      // Automatically log in the user after signup
      const loginResult = await login(email, password);
      
      if (!loginResult.success) {
        throw new Error('Account created but login failed. Please try logging in manually.');
      }
      
      // Redirect to form page after successful signup and login
      setTimeout(() => {
        navigate('/form');
      }, 1500);
    } catch (err) {
      console.error('Signup error:', err);
      setError(err.message || 'Failed to create an account');
    } finally {
      setLoading(false);
    }
  };
  
  const handleGoogleSignup = () => {
    // Construct the Google OAuth URL with a redirect_uri back to signup page
    console.log("SignupPage: Redirecting to Google OAuth");
    
    const oauth2Url = getGoogleOAuthURL('/signup');
    
    console.log("SignupPage: OAuth URL:", oauth2Url);
    window.location.href = oauth2Url;
  };
  
  return (
    <>
      <Navbar />
      
      <div className="auth-container">
        <h2>Create Your Account</h2>
        <p className="auth-subtext">Sign up and start your immigration journey today.</p>
        
        {/* Google Sign-up Button */}
        <div className="oauth-section">
          <p className="oauth-text">Quick sign-up with Google:</p>
          <button 
            type="button" 
            className="login-with-google-btn" 
            onClick={handleGoogleSignup}
          >
            <img 
              src="/Images/google_logo.png" 
              alt="Google logo" 
              className="google-icon" 
            />
            Sign up with Google
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
            placeholder="Email (required)"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoComplete="email"
          />
          
          <div className="name-fields">
            <input
              type="text"
              placeholder="First Name (required)"
              required
              value={firstName}
              onChange={(e) => setFirstName(e.target.value)}
              autoComplete="given-name"
            />
            <input
              type="text"
              placeholder="Last Name (required)"
              required
              value={lastName}
              onChange={(e) => setLastName(e.target.value)}
              autoComplete="family-name"
            />
          </div>
          
          <input
            type="password"
            placeholder="Password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="new-password"
          />
          <input
            type="password"
            placeholder="Confirm Password"
            required
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            autoComplete="new-password"
          />
          <button 
            type="submit" 
            className="btn-primary"
            disabled={loading}
          >
            {loading ? 'Creating Account...' : 'Sign Up'}
          </button>
        </form>
        
        <p className="auth-switch">
          Already have an account? <Link to="/login">Log In</Link>
        </p>
      </div>
      
      <Footer />
    </>
  );
};

export default SignupPage; 