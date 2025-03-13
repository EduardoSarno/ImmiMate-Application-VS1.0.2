import React, { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';

const OAuth2SuccessPage = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [debugInfo, setDebugInfo] = useState({});
  
  const location = useLocation();
  const navigate = useNavigate();
  
  useEffect(() => {
    console.log("OAuth2SuccessPage mounted");
    console.log("Current location:", location);
    
    // Get URL parameters - same as in the original HTML implementation
    const params = new URLSearchParams(location.search);
    const token = params.get('token');
    const email = params.get('email');
    const userId = params.get('userId');
    const errorParam = params.get('error');
    
    // Save debug info
    setDebugInfo({
      fullUrl: window.location.href,
      pathname: location.pathname,
      search: location.search,
      params: {
        token: token || 'not found',
        email: email || 'not found',
        userId: userId || 'not found',
        error: errorParam || 'not found'
      }
    });
    
    // Clear URL parameters (like in the original HTML)
    if (token || errorParam) {
      window.history.replaceState({}, document.title, window.location.pathname);
    }
    
    // Handle error case
    if (errorParam) {
      console.log("OAuth error:", errorParam);
      setLoading(false);
      setError(`Authentication Error: ${errorParam}`);
      return;
    }
    
    // Handle success case
    if (token && email) {
      console.log("OAuth success! Token and email found");
      // Store the token and user info with consistent key names
      localStorage.setItem('access_token', token);
      localStorage.setItem('user_email', email);
      if (userId) {
        localStorage.setItem('user_id', userId);
      }
      
      console.log("Saved authentication data from OAuth2 redirect");
      
      setLoading(false);
      setSuccess(true);
      
      // Auto-redirect after a delay (same as original HTML)
      setTimeout(() => {
        navigate('/form');
      }, 1500);
    } else {
      // Missing token or email
      console.log("OAuth incomplete: Missing token or email");
      setLoading(false);
      setError('Login failed: Missing authentication data');
    }
  }, [location, navigate]);

  return (
    <>
      <Navbar />
      <div style={{ padding: '40px 20px', maxWidth: '600px', margin: '0 auto', textAlign: 'center' }}>
        <h1>Authentication</h1>
        
        {loading && (
          <div style={{ margin: '20px 0', padding: '15px', backgroundColor: '#f5f5f5', borderRadius: '5px' }}>
            <p>Processing your authentication...</p>
          </div>
        )}
        
        {error && (
          <div style={{ margin: '20px 0', padding: '15px', backgroundColor: '#ffebee', borderRadius: '5px', color: '#d32f2f' }}>
            <p><strong>Error:</strong> {error}</p>
            <p>Please <Link to="/login" style={{ color: '#1976d2', textDecoration: 'underline' }}>go back to login</Link> and try again.</p>
          </div>
        )}
        
        {success && (
          <div style={{ margin: '20px 0', padding: '15px', backgroundColor: '#e8f5e9', borderRadius: '5px', color: '#2e7d32' }}>
            <p><strong>Success!</strong> Successfully logged in with Google!</p>
            <p>Redirecting you to the form page...</p>
          </div>
        )}
        
        {/* Debug information */}
        <div style={{ margin: '30px 0', padding: '15px', backgroundColor: '#e3f2fd', borderRadius: '5px', textAlign: 'left' }}>
          <h3>Debug Information</h3>
          <p><strong>Current Path:</strong> {debugInfo.pathname}</p>
          <p><strong>Query Parameters:</strong> {debugInfo.search}</p>
          <p><strong>Full URL:</strong> {debugInfo.fullUrl}</p>
          <h4>Parameters:</h4>
          <ul>
            {debugInfo.params && Object.entries(debugInfo.params).map(([key, value]) => (
              <li key={key}><strong>{key}:</strong> {value}</li>
            ))}
          </ul>
        </div>
        
        <div style={{ marginTop: '30px' }}>
          <Link to="/" style={{ display: 'inline-block', margin: '0 10px', padding: '10px 15px', backgroundColor: '#1976d2', color: 'white', textDecoration: 'none', borderRadius: '5px' }}>
            Return to Home
          </Link>
          <Link to="/login" style={{ display: 'inline-block', margin: '0 10px', padding: '10px 15px', backgroundColor: '#1976d2', color: 'white', textDecoration: 'none', borderRadius: '5px' }}>
            Back to Login
          </Link>
        </div>
      </div>
      <Footer />
    </>
  );
};

export default OAuth2SuccessPage; 