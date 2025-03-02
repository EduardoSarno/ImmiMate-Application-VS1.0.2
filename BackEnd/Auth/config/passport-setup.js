const passport = require('passport');
const GoogleStrategy = require('passport-google-oauth20').Strategy;
const User = require('../models/User');
require('dotenv').config();

passport.use(new GoogleStrategy({
  clientID: process.env.GOOGLE_CLIENT_ID,
  clientSecret: process.env.GOOGLE_CLIENT_SECRET,
  callbackURL: "/auth/google/callback"
},
async (accessToken, refreshToken, profile, done) => {
  try {
    console.log('Google profile received:', profile);  // Log the Google profile
    // Check if user already exists in our db
    let user = await User.findOne({ where: { googleId: profile.id } });
    if (user) {
      console.log('User found with Google ID:', user);  // Log if user is found
      done(null, user);
    } else {
      // Check if a user with the same email exists
      user = await User.findOne({ where: { email: profile.emails[0].value } });
      if (user) {
        console.log('User found with email, updating Google ID:', user);  // Log if user is found by email
        // Update existing user with Google ID
        user.googleId = profile.id;
        await user.save();
        done(null, user);
      } else {
        console.error('User not found, cannot authenticate:', profile.emails[0].value);  // Log if no user is found
        // If no user exists, return an error
        done(new Error('User not found'), null);
      }
    }
  } catch (error) {
    console.error('Error during Google authentication:', error);  // Log any errors
    done(error, null);
  }
}));

passport.serializeUser((user, done) => {
  done(null, user.id);
});

passport.deserializeUser(async (id, done) => {
  try {
    const user = await User.findByPk(id);
    done(null, user);
  } catch (error) {
    done(error, null);
  }
});
