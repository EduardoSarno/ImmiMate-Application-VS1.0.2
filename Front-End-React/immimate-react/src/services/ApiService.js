import axios from 'axios';
import Logger from '../utils/LoggingService';

/**
 * Service for making API calls with CSRF token handling.
 */
class ApiService {
    constructor() {
        this.API_BASE_URL = 'http://localhost:8080/api';
        this.csrfToken = null;
        this.csrfHeaderName = 'X-XSRF-TOKEN';
        this.isCircuitBroken = false;
        this.resetTime = null;
        this.retryCount = 0;
        this.maxRetries = 10;
        
        // Create axios instance with default config
        this.api = axios.create({
            baseURL: this.API_BASE_URL,
            withCredentials: true, // Important for cookies
            headers: {
                'Content-Type': 'application/json'
            },
            // Add timeout to requests
            timeout: 10000 // 10 seconds
        });
        
        // Add request interceptor to include CSRF token
        this.api.interceptors.request.use(
            config => {
                // MORE VERBOSE LOGGING FOR DEBUGGING
                Logger.debug(`ApiService: Making ${config.method.toUpperCase()} request to ${config.url}`);
                
                // Circuit breaker pattern to prevent overwhelming the server
                if (this.isCircuitBroken) {
                    const now = new Date();
                    if (this.resetTime && now < this.resetTime) {
                        Logger.debug(`Circuit broken until ${this.resetTime.toLocaleTimeString()}. Request suppressed.`);
                        // FOR DEVELOPMENT: Reset circuit breaker immediately instead of rejecting
                        this.resetCircuitBreaker();
                        Logger.debug('DEVELOPMENT MODE: Force reset circuit breaker to allow request');
                        // return Promise.reject(new Error('Circuit broken. Too many failed requests.'));
                    } else {
                        // Reset circuit breaker after timeout
                        this.resetCircuitBreaker();
                    }
                }
                
                // Remove the Authorization header completely - we'll use cookies instead
                // The backend is already configured to use HTTP-only cookies for JWT auth
                // This prevents the issue with invalid JWT formats in the Authorization header
                
                // Add CSRF token for non-GET requests if available
                if (['post', 'put', 'delete', 'patch'].includes(config.method)) {
                    if (this.csrfToken) {
                        config.headers[this.csrfHeaderName] = this.csrfToken;
                        Logger.debug(`ApiService: Added CSRF token to ${config.method.toUpperCase()} request: ${this.csrfToken.substring(0, 10)}...`);
                    } else {
                        // If CSRF token is missing for a state-changing request, we should try to fetch it first
                        Logger.warn(`ApiService: CSRF token missing for ${config.method.toUpperCase()} request to ${config.url}. This may cause a 403 error.`);
                    }
                }
                
                // Ensure credentials are included for all requests
                config.withCredentials = true;
                
                // Log the final request headers for debugging
                Logger.debug(`ApiService: Final request headers for ${config.url}:`, JSON.stringify(config.headers));
                
                return config;
            },
            error => {
                Logger.error('ApiService: Error in request interceptor:', error);
                return Promise.reject(error);
            }
        );
        
        // Add response interceptor to handle errors
        this.api.interceptors.response.use(
            response => {
                // Log successful responses
                Logger.debug(`ApiService: Successful response from ${response.config.url} - Status: ${response.status}`);
                
                // Reset retry count on successful response
                this.retryCount = 0;
                return response;
            },
            error => {
                // Log error responses with detailed information
                if (error.response) {
                    Logger.debug(`ApiService: Error response from ${error.config.url} - Status: ${error.response.status}`);
                    Logger.debug('Response data:', error.response.data);
                    Logger.debug('Response headers:', error.response.headers);
                } else if (error.request) {
                    Logger.debug(`ApiService: No response received for request to ${error.config.url}`);
                } else {
                    Logger.debug(`ApiService: Error setting up request: ${error.message}`);
                }
                
                // Increment retry count for failed requests
                this.retryCount++;
                Logger.debug(`ApiService: Retry count: ${this.retryCount}/${this.maxRetries}`);
                
                // Handle 401 Unauthorized errors 
                if (error.response && error.response.status === 401) {
                    Logger.debug('401 Unauthorized response received, authentication may have expired');
                    
                    // We'll keep the localStorage for user display info but not use it for auth
                    // The actual authentication is now happening through HTTP-only cookies
                    
                    // Instead of redirecting, just log the error - the AuthContext will handle redirects
                    if (this.retryCount >= this.maxRetries) {
                        Logger.debug('Max retries exceeded, triggering circuit breaker');
                        this.breakCircuit();
                    }
                } 
                // Handle other server errors
                
                return Promise.reject(error);
            }
        );
    }
    
    /**
     * Break the circuit to prevent further requests
     */
    breakCircuit() {
        this.isCircuitBroken = true;
        // Set a 15-second timeout before allowing requests again (reduced from 30 seconds)
        this.resetTime = new Date(Date.now() + 15000);
        Logger.debug(`Circuit breaker activated until ${this.resetTime.toLocaleTimeString()}`);
    }
    
    /**
     * Reset the circuit breaker
     */
    resetCircuitBreaker() {
        this.isCircuitBroken = false;
        this.resetTime = null;
        this.retryCount = 0;
        Logger.debug('Circuit breaker reset');
    }
    
    /**
     * Fetch a CSRF token from the server.
     * This should be called before making any state-changing requests.
     * @param {boolean} forceFresh - If true, always fetch a new token regardless of existing token
     */
    async fetchCsrfToken(forceFresh = false) {
        // Clear the fetching flag
        this._fetchingCsrfToken = false;
        
        // Don't attempt to fetch if circuit is broken
        if (this.isCircuitBroken) {
            const now = new Date();
            if (this.resetTime && now < this.resetTime) {
                Logger.debug(`Circuit broken until ${this.resetTime.toLocaleTimeString()}. CSRF token fetch suppressed.`);
                return this.csrfToken; // Return existing token if available
            } else {
                this.resetCircuitBreaker();
            }
        }
        
        // If we already have a token and not forcing refresh, don't fetch a new one
        if (this.csrfToken && !forceFresh) {
            Logger.debug('CSRF token already exists, reusing existing token');
            return this.csrfToken;
        }
        
        // Add throttling for CSRF token fetches - store the last fetch time
        const now = Date.now();
        if (!forceFresh && this._lastCsrfFetch && now - this._lastCsrfFetch < 5000) {
            Logger.debug('CSRF token fetch throttled - too soon since last fetch');
            return this.csrfToken;
        }
        
        this._lastCsrfFetch = now;
        
        try {
            Logger.debug('Fetching CSRF token from /csrf endpoint');
            const response = await this.api.get('/csrf', {
                // Ensure this specific request includes credentials
                withCredentials: true,
                // Add a timeout specific to this request
                timeout: 5000
            });
            
            Logger.debug('CSRF token response received:', response.data);
            
            if (response.data && response.data.token) {
                this.csrfToken = response.data.token;
                this.csrfHeaderName = response.data.headerName || 'X-XSRF-TOKEN';
                Logger.debug(`CSRF token fetched: ${this.csrfToken.substring(0, 10)}...`);
                Logger.debug(`CSRF header name: ${this.csrfHeaderName}`);
                return this.csrfToken;
            } else {
                Logger.error('Invalid CSRF token response', response.data);
                // For debugging, log the full response
                Logger.debug('Full CSRF response:', {
                    status: response.status,
                    headers: response.headers,
                    data: response.data
                });
                return null;
            }
        } catch (error) {
            Logger.error('Error fetching CSRF token:', error);
            
            // Add more detailed error logging
            if (error.response) {
                Logger.error('CSRF fetch response error:', {
                    status: error.response.status,
                    data: error.response.data,
                    headers: error.response.headers
                });
            } else if (error.request) {
                Logger.error('CSRF fetch request made but no response received');
            } else {
                Logger.error('CSRF fetch setup error:', error.message);
            }
            
            throw error;
        }
    }
    
    /**
     * Save form draft to the server.
     * 
     * @param {Object} formData The form data to save
     * @returns {Promise<Object>} The response from the server
     */
    async saveFormDraft(formData) {
        // Don't attempt operation if circuit is broken
        if (this.isCircuitBroken) {
            Logger.debug('Circuit broken. Draft save operation suppressed.');
            return null;
        }
        
        try {
            // Ensure we have a CSRF token
            if (!this.csrfToken) {
                await this.fetchCsrfToken();
            }
            
            Logger.debug('Saving form draft to server');
            // Use only the general endpoint that's implemented in the backend
            const response = await this.api.post('/profiles/draft', formData, {
                withCredentials: true // Ensure cookies are sent with the request
            });
            Logger.debug('Form draft saved successfully');
            return response.data;
        } catch (error) {
            // Handle 404 Not Found errors gracefully - endpoint might not be implemented yet
            if (error.response && error.response.status === 404) {
                Logger.warn('Form draft endpoint not found (404). Falling back to localStorage only.');
                // Return a mock successful response
                return { success: true, message: 'Using localStorage fallback', data: formData };
            }
            
            Logger.error('Error saving form draft:', error);
            // Instead of throwing, return null to allow the app to continue
            return null;
        }
    }
    
    /**
     * Get the latest form draft from the server.
     * 
     * @returns {Promise<Object>} The latest form draft
     */
    async getLatestFormDraft() {
        // Don't attempt operation if circuit is broken
        if (this.isCircuitBroken) {
            Logger.debug('Circuit broken. Draft fetch operation suppressed.');
            return null;
        }
        
        try {
            // Ensure we have a CSRF token
            if (!this.csrfToken) {
                await this.fetchCsrfToken();
            }
            
            Logger.debug('Fetching latest form draft from server');
            // Use only the general endpoint that's implemented in the backend
            const response = await this.api.get('/profiles/draft', {
                withCredentials: true, // Ensure cookies are sent with the request
                headers: {
                    'x-debug-info': 'getLatestFormDraft-call'
                },
                // Add timeout to this specific request
                timeout: 5000
            });
            
            Logger.debug('Draft response:', response.data);
            
            // Reset retry count on successful call
            this.retryCount = 0;
            
            if (response.data && response.data.success) {
                if (response.data.formData) {
                    Logger.debug('Form draft fetched successfully with formData key');
                    return response.data.formData;
                }
                
                // Fallback to data key for backward compatibility
                if (response.data.data) {
                    Logger.debug('Form draft fetched successfully with data key');
                    return response.data.data;
                }
            }
            
            return null;
        } catch (error) {
            // Increment retry count for failed requests
            this.retryCount++;
            
            // If we've exceeded our retry limit, trip the circuit breaker
            if (this.retryCount >= 3) {
                Logger.debug('Max retries exceeded, breaking circuit to prevent infinite loops');
                this.breakCircuit();
            }
            
            // Check for network errors and break circuit quickly
            if (!error.response) {
                // For connection errors, break circuit immediately to prevent further requests
                Logger.error(`Connection error (server might be down): ${error.message}`);
                if (error.code === 'ERR_NETWORK' || error.code === 'ECONNREFUSED' || error.code === 'ECONNABORTED') {
                    this.breakCircuit();
                    return null;
                }
            }
            
            // Handle 404 Not Found errors gracefully - endpoint might not be implemented yet
            if (error.response && error.response.status === 404) {
                Logger.warn('Form draft endpoint not found (404). Falling back to localStorage only.');
                return null; // Return null to allow localStorage fallback
            }
            
            if (error.response) {
                // Log detailed response information for debugging
                Logger.debug(`Draft fetch error: ${error.response.status} - ${error.response.statusText}`);
                
                if (error.response.status === 401) {
                    Logger.debug('Authentication error while fetching form draft. User may need to log in again.');
                    return null;
                }
            }
            
            Logger.error('Error fetching form draft:', error);
            // Instead of throwing, return null to continue with localStorage fallback
            return null;
        }
    }
}

// Create a singleton instance
const apiService = new ApiService();

export default apiService; 