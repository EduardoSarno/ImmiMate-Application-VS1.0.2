/**
 * Application Configuration
 */

// API Configuration
export const API_BASE_URL = 'http://localhost:8080/api';

// Feature Flags
export const FEATURES = {
  LANGUAGE_TEST_CLIENT_FALLBACK: true, // Enable client-side fallback for language test conversions
  AUTO_SAVE_DRAFTS: true
};

// Timeouts
export const TIMEOUTS = {
  API_REQUEST: 30000, // 30 seconds
  SESSION_IDLE: 3600000 // 1 hour
}; 