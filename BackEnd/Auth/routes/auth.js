// BackEnd/Auth/routes/auth.js
const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const User = require('../models/User');
const passport = require('passport');
require('../config/passport-setup');
require('dotenv').config();

const router = express.Router();

// Sign-Up Endpoint
router.post('/signup', async (req, res) => {
  const { email, password } = req.body;
  try {
    // Check if user already exists
    const existingUser = await User.findOne({ where: { email } });
    if (existingUser) return res.status(400).json({ message: 'User already exists' });

    // Hash the password
    const hashedPassword = await bcrypt.hash(password, 10);

    // Create a new user
    const user = await User.create({ email, password: hashedPassword });

    // Generate a JWT token
    const token = jwt.sign({ id: user.id }, process.env.JWT_SECRET, { expiresIn: '1h' });

    res.status(201).json({ token });
  } catch (error) {
    res.status(500).json({ message: 'Server error' });
  }
});

// Login Endpoint
router.post('/login', async (req, res) => {
  const { email, password } = req.body;
  try {
    console.log('Request body:', req.body);  // Log the request body to verify contents
    // Retrieve user data from the database
    const user = await User.findOne({ where: { email } });
    if (!user) {
      console.error('User not found:', email);  // Log if user is not found
      return res.status(400).json({ message: 'User not found' });
    }

    // Compare the hashed password
    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) {
      console.error('Invalid credentials for user:', email);  // Log if password does not match
      return res.status(400).json({ message: 'Invalid credentials' });
    }
    console.log('Login successful:', user.email);

    // Generate a JWT token
    const token = jwt.sign({ id: user.id }, process.env.JWT_SECRET, { expiresIn: '1h' });

    // Return the token AND user ID so it can be stored in localStorage
    res.json({ 
      token,
      userId: user.id,
      userEmail: user.email
    });
  } catch (error) {
    console.error("Error during login:", error);  // Log the error details
    res.status(500).json({ message: 'Server error' });
  }
});

// Google Login Endpoint
router.get('/google', passport.authenticate('google', {
  scope: ['profile', 'email']
}));

// Google Callback Endpoint
router.get('/google/callback', passport.authenticate('google', { failureRedirect: '/' }),
  async (req, res) => {
    const { email, id: googleId } = req.user;
    console.log('Google user authenticated:', { email, googleId });  // Log user info
    console.log('Email from Google profile:', email); // Log email from profile
    try {
      let user = await User.findOne({ where: { googleId } });
      if (!user) {
        // Check if a user with the same email exists
        user = await User.findOne({ where: { email } });
        if (user) {
          // Update existing user with Google ID
          user.googleId = googleId;
          await user.save();
        } else {
          // If no user exists, redirect with an error
          console.error('User not found during Google login:', email);  // Log error
          return res.redirect('/?error=user_not_found');
        }
      }
      const token = jwt.sign({ id: user.id }, process.env.JWT_SECRET, { expiresIn: '1h' });
      console.log('JWT token generated:', token);  // Log token
      console.log('Logged in as:', email);  // Log the user's email after successful authentication
      console.log('User ID:', user.id);  // Log the user's ID
      console.log('Redirecting with email:', email); // Log before redirect with email
      
      // Include user ID in the redirect URL
      res.redirect(`/auth/success?token=${token}&email=${encodeURIComponent(email)}&userId=${user.id}`);  // Redirect to a new route
    } catch (error) {
      console.error('Error during Google log-in:', error);  // Log error
      res.redirect('/?error=server');
    }
  }
);

module.exports = router;