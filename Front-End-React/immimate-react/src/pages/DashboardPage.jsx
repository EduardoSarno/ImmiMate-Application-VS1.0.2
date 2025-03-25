import React from 'react';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import { useAuth } from '../contexts/AuthContext';

// Dashboard styles
const dashboardStyles = {
  dashboardContainer: {
    maxWidth: '1200px',
    margin: '0 auto',
    padding: '40px 20px',
  },
  dashboardHeader: {
    marginBottom: '40px',
    textAlign: 'center',
  },
  dashboardHeaderTitle: {
    color: 'var(--primary-color)',
    marginBottom: '10px',
  },
  dashboardContent: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
    gap: '20px',
  },
  dashboardCard: {
    backgroundColor: 'white',
    borderRadius: '10px',
    padding: '25px',
    boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
    margin: '10px 0',
    border: '1px solid #e0e0e0',
  },
  dashboardCardTitle: {
    color: 'var(--primary-color)',
    marginBottom: '20px',
    fontSize: '22px',
  },
  statusIndicator: {
    display: 'flex',
    alignItems: 'center',
    margin: '15px 0',
  },
  statusCircle: {
    width: '18px',
    height: '18px',
    borderRadius: '50%',
    marginRight: '10px',
  },
  inProgress: {
    backgroundColor: '#ffcc00',
  },
  scoreDisplay: {
    fontSize: '46px',
    fontWeight: 'bold',
    color: 'var(--primary-color)',
    margin: '15px 0',
  },
  scoreMax: {
    fontSize: '24px',
    color: '#666',
  },
  programList: {
    listStyleType: 'none',
    padding: 0,
  },
  programListItem: {
    padding: '12px 0',
    borderBottom: '1px solid var(--light-gray)',
  },
  programListItemLast: {
    padding: '12px 0',
    borderBottom: 'none',
  }
};

const DashboardPage = () => {
  const { currentUser } = useAuth();
  
  return (
    <>
      <Navbar />
      <div style={dashboardStyles.dashboardContainer}>
        <h1>Dashboard</h1>
        <p>Welcome, {currentUser?.email || 'Guest'}! This is your personal dashboard where you can view and manage your immigration process.</p>
        
        <div style={dashboardStyles.dashboardContent}>
          <div style={dashboardStyles.dashboardCard}>
            <h3>Application Status</h3>
            <p>Your application is currently being processed.</p>
          </div>
          
          <div style={dashboardStyles.dashboardCard}>
            <h3>Important Dates</h3>
            <p>No upcoming deadlines at this time.</p>
          </div>
          
          <div style={dashboardStyles.dashboardCard}>
            <h3>Documents</h3>
            <p>All required documents have been submitted.</p>
          </div>
          
          <div style={dashboardStyles.dashboardCard}>
            <h3>Messages</h3>
            <p>You have no unread messages.</p>
          </div>
        </div>
      </div>
      <Footer />
    </>
  );
};

export default DashboardPage; 