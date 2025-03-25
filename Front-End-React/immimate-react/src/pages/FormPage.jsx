import React from 'react';
import { Navigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import ProfileForm from '../components/profile/ProfileForm';
import { useAuth } from '../contexts/AuthContext';
import '../styles/FormPage.css';

const FormPage = () => {
  const { currentUser } = useAuth();
  
  // If no user is logged in, redirect to login page
  if (!currentUser) {
    return <Navigate to="/login" />;
  }
  
  return (
    <div className="form-page">
      <Navbar />
      
      <div className="page-container">
        <div className="form-page-content" style={{ maxWidth: "none", width: "100%" }}>
          <div className="form-intro">
            <h1>Your Immigration Journey Starts Here</h1>
            <p>
              Please fill out this form to help us understand your immigration needs.
              We will use this information to provide you with tailored advice and options.
            </p>
          </div>

          <ProfileForm />
        </div>
      </div>
      
      <Footer />
    </div>
  );
};

export default FormPage; 