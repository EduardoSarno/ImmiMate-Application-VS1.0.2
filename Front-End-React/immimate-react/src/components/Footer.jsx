import React from 'react';
import { Link } from 'react-router-dom';

const Footer = () => {
  return (
    <footer className="main-footer">
      <h2>Your Trusted AI Immigration Assistant</h2>
      <p>Simplifying your journey to Canadian Permanent Residency with personalized guidance, integrated data analysis, and AI-driven solutions.</p>
      
      <p className="footer-contact">Contact Us: contact@immimate.co</p>

      <div className="footer-links">
        <Link to="/privacy-policy">Privacy Policy</Link> |
        <Link to="/terms">Terms & Conditions</Link>
      </div>

      <p className="footer-copyright">© 2024 ImmiMate. All rights reserved.</p>
    </footer>
  );
};

export default Footer; 