// BackEnd/server.js
const express = require('express');
const session = require('express-session');
const passport = require('passport');
const sequelize = require('./Auth/config/database');
const User = require('./Auth/models/User');
const authRoutes = require('./Auth/routes/auth');
const authenticateJWT = require('./Auth/middleware/authMiddleware');
const cors = require('cors');
const userImmigrationProfileRoutes = require('./Auth/models/userImmigrationProfile');
const path = require('path');

const app = express();

// Middleware to parse JSON bodies
app.use(express.json());

// Middleware to parse URL-encoded bodies
app.use(express.urlencoded({ extended: true }));

// CORS configuration
app.use(cors({
  origin: ['http://localhost:3000'],
  credentials: true
}));

// Session handling
app.use(session({
  secret: process.env.SESSION_SECRET,
  resave: false,
  saveUninitialized: true
}));

// Initialize Passport
app.use(passport.initialize());
app.use(passport.session());

// Serve static files from the Front-End directory
app.use(express.static(path.join(__dirname, '../Front-End')));

// Use authentication routes
app.use('/auth', authRoutes);

// Example of a protected route
app.get('/protected', authenticateJWT, (req, res) => {
  res.json({ message: 'This is a protected route', user: req.user });
});

// Route to serve index.html at the root path
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, '../Front-End/Html/index.html'));
});

app.get('/auth/success', (req, res) => {
  const token = req.query.token;
  const email = req.query.email;
  const userId = req.query.userId;
  console.log('Received token in /auth/success:', token);
  console.log('Received email in /auth/success:', email);
  console.log('Received userId in /auth/success:', userId);
  res.redirect(`/Html/log-in.html?token=${token}&email=${encodeURIComponent(email)}&userId=${userId}`);
});

// Use user immigration profile routes
app.use('/profile', userImmigrationProfileRoutes);

// Sync the database
sequelize.sync().then(() => {
  console.log('Good to go!');
});

app.listen(3000, () => {
  console.log('Server is running on port 3000');
});