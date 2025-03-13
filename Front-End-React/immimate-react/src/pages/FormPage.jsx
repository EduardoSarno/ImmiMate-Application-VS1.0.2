import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import ProfileForm from '../components/profile/ProfileForm';
import '../styles/FormPage.css';

const FormPage = () => {
  const [profileSubmitted, setProfileSubmitted] = useState(false);
  // Remove unused variable or add a comment explaining why it's needed
  // const { currentUser } = useAuth();
  const navigate = useNavigate();

  // Check if user already submitted a profile
  useEffect(() => {
    const hasSubmittedProfile = localStorage.getItem('has_submitted_profile') === 'true';
    setProfileSubmitted(hasSubmittedProfile);
    
    // If profile is already submitted, redirect to dashboard
    if (hasSubmittedProfile) {
      setTimeout(() => {
        navigate('/dashboard');
      }, 3000); // Redirect after 3 seconds
    }
  }, [navigate]);

  return (
    <div className="form-page">
      <Navbar />
      
      <div className="page-container">
        {profileSubmitted ? (
          <div className="notification success-notification" style={{ maxWidth: '800px', margin: '40px auto' }}>
            <h3>Profile Already Submitted</h3>
            <p>You have already submitted your immigration profile. Redirecting to your dashboard...</p>
            <button 
              onClick={() => navigate('/dashboard')} 
              className="btn-primary"
              style={{ marginTop: '20px', padding: '10px 20px' }}
            >
              Go to Dashboard Now
            </button>
          </div>
        ) : (
          <div className="form-page-content" style={{ maxWidth: "none", width: "100%" }}>
            <div className="form-intro">
              <h1>Immigration Profile Form</h1>
              <p>
                Complete this form to assess your eligibility for Canadian immigration programs.
                The information you provide will help us determine the best immigration pathway for you.
              </p>
              <p className="form-instructions">
                Please fill out all required fields (marked with <span className="required-indicator">*</span>).
                Your data is secure and will only be used for immigration assessment purposes.
              </p>
            </div>

            <ProfileForm />
          </div>
        )}
      </div>
      
      <Footer />
    </div>
  );
};

export default FormPage; 