/**
 * loginHandler.js - Handles user login information storage
 */

document.addEventListener('DOMContentLoaded', function() {
  // Parse URL parameters to get token, email and userId
  const urlParams = new URLSearchParams(window.location.search);
  const token = urlParams.get('token');
  const email = urlParams.get('email');
  const userId = urlParams.get('userId');
  
  console.log("Login parameters:", { token, email, userId });
  
  // If we have a token from the URL, store it in localStorage
  if (token) {
    localStorage.setItem("access_token", token);
    console.log("Token stored in localStorage");
    
    // Clear the token from the URL (for security)
    if (window.history && window.history.replaceState) {
      const cleanUrl = window.location.href.split('?')[0];
      window.history.replaceState({}, document.title, cleanUrl);
    }
  }
  
  // Store email if provided
  if (email) {
    localStorage.setItem("user_email", email);
    console.log("Email stored in localStorage:", email);
  }
  
  // Store userId if provided
  if (userId) {
    localStorage.setItem("user_id", userId);
    console.log("User ID stored in localStorage:", userId);
  }
  
  // Handle login form submission (for manual login)
  const loginForm = document.getElementById('loginForm');
  if (loginForm) {
    loginForm.addEventListener('submit', async function(e) {
      e.preventDefault();
      
      const email = document.getElementById('email').value;
      const password = document.getElementById('password').value;
      
      try {
        const response = await fetch('http://localhost:3000/auth/login', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ email, password })
        });
        
        const data = await response.json();
        
        if (data.token) {
          localStorage.setItem('access_token', data.token);
          console.log("Token stored from login form");
          
          if (data.userEmail) {
            localStorage.setItem('user_email', data.userEmail);
            console.log("Email stored from login form:", data.userEmail);
          }
          
          if (data.userId) {
            localStorage.setItem('user_id', data.userId);
            console.log("User ID stored from login form:", data.userId);
          }
          
          // Redirect to dashboard after successful login
          window.location.href = 'dashboard.html';
        } else {
          alert(data.message || 'Login failed');
        }
      } catch (error) {
        console.error("Login error:", error);
        alert('Login failed. Please try again.');
      }
    });
  }
  
  // Check if user is logged in
  const checkLoginStatus = () => {
    const token = localStorage.getItem('access_token');
    const email = localStorage.getItem('user_email');
    const userId = localStorage.getItem('user_id');
    
    console.log("Checking login status:", { 
      hasToken: !!token, 
      hasEmail: !!email,
      hasUserId: !!userId
    });
    
    // If we're on a page that requires login and there's no token, redirect to login
    const requiresLogin = document.body.classList.contains('requires-login');
    if (requiresLogin && !token) {
      console.log("Login required but no token found, redirecting");
      window.location.href = 'log-in.html';
    }
    
    // Update UI elements based on login status
    const loggedInElements = document.querySelectorAll('.logged-in-only');
    const loggedOutElements = document.querySelectorAll('.logged-out-only');
    
    if (token) {
      // User is logged in
      loggedInElements.forEach(el => el.style.display = 'block');
      loggedOutElements.forEach(el => el.style.display = 'none');
      
      // Display user email if there's an element for it
      const userEmailElements = document.querySelectorAll('.user-email');
      userEmailElements.forEach(el => {
        el.textContent = email || 'User';
      });
    } else {
      // User is logged out
      loggedInElements.forEach(el => el.style.display = 'none');
      loggedOutElements.forEach(el => el.style.display = 'block');
    }
  };
  
  // Run the check on page load
  checkLoginStatus();
}); 