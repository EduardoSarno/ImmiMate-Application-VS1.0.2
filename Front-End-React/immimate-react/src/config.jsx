/**
 * Application configuration
 */

// Backend server URL
export const API_BASE_URL = 'http://localhost:8080';

// Other config variables can be added here
export const APP_NAME = 'ImmiMate';

/**
 * Get the URL for OAuth authorization with Google
 * @param {string} redirectPath - Where to redirect after successful OAuth (e.g., '/login' or '/signup')
 * @returns {string} The full URL for OAuth authorization
 */
export function getGoogleOAuthURL(redirectPath) {
  // Build the redirect URI (the frontend page that will handle the OAuth callback)
  const redirectUri = `${window.location.origin}${redirectPath}`;
  
  // Construct the full OAuth URL with the backend endpoint
  return `${API_BASE_URL}/api/oauth2/authorization/google?redirect_uri=${encodeURIComponent(redirectUri)}`;
}

/**
 * Process OAuth callback parameters from the URL
 * @param {URLSearchParams} params - The URL search parameters
 * @returns {Object} The processed OAuth data containing token, email, error etc.
 */
export function processOAuthCallback(params) {
  const token = params.get('token');
  const email = params.get('email');
  const userId = params.get('userId');
  const error = params.get('error');
  
  console.log("processOAuthCallback: Processing OAuth parameters");
  console.log("processOAuthCallback: Token present:", token ? `Yes (length: ${token.length})` : "No");
  console.log("processOAuthCallback: Email:", email);
  console.log("processOAuthCallback: UserId:", userId);
  console.log("processOAuthCallback: Error:", error || "None");
  
  return {
    token,
    email,
    userId,
    error
  };
}

/**
 * Clear OAuth parameters from the URL to prevent reprocessing on page refresh
 * @param {string} basePath - The base path to reset to (e.g., '/login')
 */
export function clearOAuthParams(basePath) {
  window.history.replaceState({}, document.title, basePath);
} 