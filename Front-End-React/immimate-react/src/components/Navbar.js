import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const Navbar = () => {
  const { currentUser, logout } = useAuth();
  const location = useLocation();
  const [isNavOpen, setIsNavOpen] = useState(false);
  const [hasSubmittedProfile, setHasSubmittedProfile] = useState(false);
  
  // Check if user has submitted a profile
  useEffect(() => {
    const checkProfileStatus = () => {
      const profileSubmitted = localStorage.getItem('has_submitted_profile') === 'true';
      setHasSubmittedProfile(profileSubmitted);
    };
    
    checkProfileStatus();
    
    // Add event listener for storage changes in case profile status changes in another tab
    window.addEventListener('storage', checkProfileStatus);
    
    // Also check when the component re-renders if user is logged in
    if (currentUser) {
      checkProfileStatus();
    }
    
    return () => {
      window.removeEventListener('storage', checkProfileStatus);
    };
  }, [currentUser]);
  
  const isActive = (path) => {
    return location.pathname === path ? 'active' : '';
  };
  
  const handleLogout = (e) => {
    e.preventDefault();
    logout();
  };
  
  const toggleNav = () => {
    setIsNavOpen(!isNavOpen);
  };
  
  const closeNav = () => {
    setIsNavOpen(false);
  };
  
  return (
    <nav className={`navbar ${isNavOpen ? 'nav-open' : ''}`}>
      <div className="nav-brand">
        <Link to="/" onClick={closeNav}>
          <img 
            src="/logo.png" 
            alt="ImmiMate Logo" 
            className="logo" 
          />
          <span className="nav-logo">ImmiMate</span>
        </Link>
      </div>
      
      <div className="hamburger" onClick={toggleNav}>
        <span></span>
        <span></span>
        <span></span>
        <span></span>
      </div>
      
      <div className="nav-links">
        <Link to="/" className={`nav-item ${isActive('/')}`} onClick={closeNav}>Home</Link>
        {currentUser && (
          <>
            {!hasSubmittedProfile && (
              <Link to="/form" className={`nav-item ${isActive('/form')}`} onClick={closeNav}>Eligibility Form</Link>
            )}
            <Link to="/dashboard" className={`nav-item ${isActive('/dashboard')}`} onClick={closeNav}>Dashboard</Link>
            <Link to="/chat" className={`nav-item ${isActive('/chat')}`} onClick={closeNav}>Chat with Mate</Link>
          </>
        )}
      </div>
      
      <div className="auth-buttons">
        {currentUser ? (
          <div className="logged-in-only">
            <span className="user-email">{currentUser.email}</span>
            <Link to="/" onClick={(e) => { closeNav(); handleLogout(e); }} className="sign-out">Log Out</Link>
          </div>
        ) : (
          <>
            <Link to="/login" className="sign-in" onClick={closeNav}>Log In</Link>
            <Link to="/signup" className="sign-up" onClick={closeNav}>Sign Up</Link>
          </>
        )}
      </div>
    </nav>
  );
};

export default Navbar; 