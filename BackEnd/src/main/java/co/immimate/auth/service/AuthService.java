package co.immimate.auth.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import co.immimate.auth.dto.LoginRequest;
import co.immimate.auth.dto.RegisterRequest;
import co.immimate.auth.security.JwtUtil;
import co.immimate.user.model.User;
import co.immimate.user.repository.UserRepository;

/**
 * Service for handling authentication and registration
 */
@Service
public class AuthService {

    // Logger
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    // User roles
    private static final String ROLE_USER = "USER";
    
    // Authentication principals
    private static final String ANONYMOUS_USER = "anonymousUser";
    
    // OAuth2 providers
    private static final String OAUTH2_PROVIDER_GOOGLE = "google";
    
    // OAuth2 attribute keys
    private static final String OAUTH2_ATTR_EMAIL = "email";
    private static final String OAUTH2_ATTR_SUB = "sub";
    
    // Response keys
    private static final String RESPONSE_SUCCESS = "success";
    private static final String RESPONSE_MESSAGE = "message";
    private static final String RESPONSE_DATA = "data";
    private static final String RESPONSE_EMAIL = "email";
    private static final String RESPONSE_USER_ID = "userId";
    private static final String RESPONSE_ID = "id";
    private static final String RESPONSE_ROLE = "role";
    private static final String RESPONSE_FIRST_NAME = "firstName";
    private static final String RESPONSE_LAST_NAME = "lastName";
    
    // Success messages
    private static final String SUCCESS_AUTH = "Authentication successful";
    private static final String SUCCESS_REGISTER = "User registered successfully";
    private static final String SUCCESS_USER_DETAILS = "User details retrieved successfully";
    private static final String SUCCESS_LOGOUT = "Logged out successfully";
    
    // Error messages
    private static final String ERROR_USER_NOT_FOUND = "User not found";
    private static final String ERROR_INVALID_CREDENTIALS = "Invalid email or password";
    private static final String ERROR_AUTH_FAILED = "Authentication failed";
    private static final String ERROR_MISSING_EMAIL_PHONE = "Either email or phone number must be provided";
    private static final String ERROR_EMAIL_IN_USE = "Email is already in use";
    private static final String ERROR_REGISTRATION_FAILED = "Registration failed: ";
    private static final String ERROR_NO_AUTH_USER = "No authenticated user found";
    private static final String ERROR_USER_DETAILS = "Error getting user details: ";
    private static final String ERROR_LOGOUT_FAILED = "Logout failed: ";
    
    // Log messages
    private static final String LOG_INVALID_CREDENTIALS = "Invalid credentials: {}";
    private static final String LOG_AUTH_ERROR = "Authentication error: {}";
    private static final String LOG_REGISTRATION_ERROR = "Registration error: {}";
    private static final String LOG_NO_AUTH_USER = "getCurrentUser: No authenticated user found";
    private static final String LOG_USER_FROM_DETAILS = "getCurrentUser: Found authenticated user from UserDetails: {}";
    private static final String LOG_AUTH_USER = "getCurrentUser: Found authenticated user: {}";
    private static final String LOG_EMAIL_FROM_OAUTH2 = "getCurrentUser: Extracted email from OAuth2 attributes: {}";
    private static final String LOG_USER_BY_EMAIL = "getCurrentUser: Looked up user by email {}, found: {}";
    private static final String LOG_USER_BY_GOOGLE_ID = "getCurrentUser: Looked up user by Google ID {}, found: {}";
    private static final String LOG_USER_BY_PRINCIPAL = "getCurrentUser: Looked up user by principal as Google ID {}, found: {}";
    private static final String LOG_USER_NOT_FOUND = "getCurrentUser: User not found in database: {}";
    private static final String LOG_CURRENT_USER_ERROR = "Error getting current user: {}";
    private static final String LOG_LOGOUT_ERROR = "Logout error: {}";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Authenticates a user with email and password
     *
     * @param loginRequest Login request containing email and password
     * @param response HTTP response to add cookies
     * @return User details on successful authentication (token is sent in cookie)
     */
    public ResponseEntity<?> authenticateUser(LoginRequest loginRequest, HttpServletResponse response) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Get user from database
            Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(createErrorResponse(ERROR_USER_NOT_FOUND));
            }

            User user = userOpt.get();

            // Add JWT token as HttpOnly cookie
            jwtUtil.addJwtCookieToResponse(response, user);

            // Return success with user details (without token in response body)
            Map<String, Object> responseData = new HashMap<>();
            responseData.put(RESPONSE_EMAIL, user.getEmail());
            responseData.put(RESPONSE_USER_ID, user.getId().toString());
            responseData.put(RESPONSE_ROLE, user.getRole());
            
            return ResponseEntity.ok(createSuccessResponse(SUCCESS_AUTH, responseData));
        } catch (BadCredentialsException e) {
            logger.error(LOG_INVALID_CREDENTIALS, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(createErrorResponse(ERROR_INVALID_CREDENTIALS));
        } catch (AuthenticationException e) {
            // All other authentication exceptions
            logger.error(LOG_AUTH_ERROR, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(createErrorResponse(ERROR_AUTH_FAILED));
        } catch (DataAccessException e) {
            // Database related exceptions
            logger.error(LOG_AUTH_ERROR, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(ERROR_AUTH_FAILED));
        } catch (Exception e) {
            // Fallback for any other unexpected exceptions
            logger.error(LOG_AUTH_ERROR, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(ERROR_AUTH_FAILED));
        }
    }

    /**
     * Registers a new user
     *
     * @param registerRequest Registration request containing user details
     * @return Success message on successful registration
     */
    public ResponseEntity<?> registerUser(RegisterRequest registerRequest) {
        try {
            // Check if the request is valid (either email or phone is provided)
            if (!registerRequest.isValid()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse(ERROR_MISSING_EMAIL_PHONE));
            }
            
            // Check if email already exists (if email is provided)
            if (registerRequest.getEmail() != null && !registerRequest.getEmail().trim().isEmpty()) {
                if (userRepository.existsByEmail(registerRequest.getEmail())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(createErrorResponse(ERROR_EMAIL_IN_USE));
                }
            }
            
            // Create new user
            User user = new User();
            
            // Set required fields
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            user.setRole(ROLE_USER); // Default role
            user.setCreatedAt(Instant.now());
            user.setUpdatedAt(Instant.now());
            
            // Set optional fields if provided
            if (registerRequest.getEmail() != null) {
                user.setEmail(registerRequest.getEmail());
            }
            
            if (registerRequest.getPhoneNumber() != null) {
                user.setPhoneNumber(registerRequest.getPhoneNumber());
            }
            
            // Handle optional name fields
            String firstName = registerRequest.getFirstName();
            String lastName = registerRequest.getLastName();
            
            if (firstName != null) {
                user.setFirstName(firstName);
            }
            
            if (lastName != null) {
                user.setLastName(lastName);
            }
            
            // Set full name if either first or last name is provided
            if (firstName != null || lastName != null) {
                String fullName = 
                    (firstName != null ? firstName : "") + 
                    ((firstName != null && lastName != null) ? " " : "") +
                    (lastName != null ? lastName : "");
                user.setName(fullName.trim());
            }

            // Save user to database
            userRepository.save(user);

            return ResponseEntity.ok(createSuccessResponse(SUCCESS_REGISTER, null));
        } catch (Exception e) {
            logger.error(LOG_REGISTRATION_ERROR, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(ERROR_REGISTRATION_FAILED + e.getMessage()));
        }
    }

    /**
     * Gets the current authenticated user
     *
     * @return User details
     */
    public ResponseEntity<?> getCurrentUser() {
        try {
            // Get the current authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals(ANONYMOUS_USER)) {
                logger.warn(LOG_NO_AUTH_USER);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse(ERROR_NO_AUTH_USER));
            }
            
            // Get user details from authentication
            String email = null;
            
            // Check if principal is a UserDetails object (from our UserDetailsService)
            if (authentication.getPrincipal() instanceof UserDetails userDetails) {
                email = userDetails.getUsername();
                logger.debug(LOG_USER_FROM_DETAILS, email);
            } else {
                // For OAuth2 auth, the principal might be a name/ID instead of UserDetails
                String principal = authentication.getName();
                logger.debug(LOG_AUTH_USER, principal);
                
                // Try to find user by email first (the most reliable way)
                if (principal.contains("@")) {
                    email = principal;
                } else {
                    // Try to get the email from OAuth2 authentication if applicable
                    if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
                        Map<String, Object> attributes = oauthToken.getPrincipal().getAttributes();
                        email = (String) attributes.get(OAUTH2_ATTR_EMAIL);
                        logger.debug(LOG_EMAIL_FROM_OAUTH2, email);
                    }
                }
            }
            
            // Get user from database
            Optional<User> userOpt = Optional.empty();
            if (email != null) {
                // Try to find by email first (most reliable)
                userOpt = userRepository.findByEmail(email);
                logger.debug(LOG_USER_BY_EMAIL, email, userOpt.isPresent());
            } 
            
            if (!userOpt.isPresent()) {
                // Try to find by Google ID if email lookup failed
                String principal = authentication.getName();
                
                // If this is an OAuth authentication with Google
                if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
                    if (OAUTH2_PROVIDER_GOOGLE.equals(oauthToken.getAuthorizedClientRegistrationId())) {
                        // Extract the Google ID (sub) from the attributes
                        Map<String, Object> attributes = oauthToken.getPrincipal().getAttributes();
                        String googleId = (String) attributes.get(OAUTH2_ATTR_SUB);
                        
                        if (googleId != null) {
                            userOpt = userRepository.findByGoogleId(googleId);
                            logger.debug(LOG_USER_BY_GOOGLE_ID, 
                                          googleId, userOpt.isPresent());
                        }
                    }
                } else if (!principal.contains("@")) {
                    // If principal is not an email, it might be a Google ID
                    userOpt = userRepository.findByGoogleId(principal);
                    logger.debug(LOG_USER_BY_PRINCIPAL, 
                                  principal, userOpt.isPresent());
                }
            }
            
            if (userOpt.isEmpty()) {
                logger.warn(LOG_USER_NOT_FOUND, authentication.getName());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse(ERROR_USER_NOT_FOUND));
            }
            
            User user = userOpt.get();
            
            // Create response with user details
            Map<String, Object> userData = new HashMap<>();
            userData.put(RESPONSE_EMAIL, user.getEmail());
            userData.put(RESPONSE_ID, user.getId().toString());
            userData.put(RESPONSE_ROLE, user.getRole());
            userData.put(RESPONSE_FIRST_NAME, user.getFirstName());
            userData.put(RESPONSE_LAST_NAME, user.getLastName());
            
            return ResponseEntity.ok(createSuccessResponse(SUCCESS_USER_DETAILS, userData));
        } catch (Exception e) {
            logger.error(LOG_CURRENT_USER_ERROR, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(ERROR_USER_DETAILS + e.getMessage()));
        }
    }
    
    /**
     * Logs out the current user by clearing the JWT cookie
     *
     * @param response HTTP response to clear cookies
     * @return Success message
     */
    public ResponseEntity<?> logoutUser(HttpServletResponse response) {
        try {
            // Clear the JWT cookie
            jwtUtil.addClearJwtCookieToResponse(response);
            
            // Clear the authentication context
            SecurityContextHolder.clearContext();
            
            return ResponseEntity.ok(createSuccessResponse(SUCCESS_LOGOUT, null));
        } catch (Exception e) {
            logger.error(LOG_LOGOUT_ERROR, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(ERROR_LOGOUT_FAILED + e.getMessage()));
        }
    }
    
    /**
     * Verifies if the current JWT token is valid
     *
     * @return User details if token is valid, error if not
     */
    public ResponseEntity<?> verifyCurrentToken() {
        // This method simply delegates to getCurrentUser because
        // the JWT filter will already validate the token
        return getCurrentUser();
    }
    
    /**
     * Creates a standardized success response
     *
     * @param message Success message
     * @param data Response data
     * @return Map with success flag, message, and data
     */
    private Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put(RESPONSE_SUCCESS, true);
        response.put(RESPONSE_MESSAGE, message);
        if (data != null) {
            response.put(RESPONSE_DATA, data);
        }
        return response;
    }
    
    /**
     * Creates a standardized error response
     *
     * @param message Error message
     * @return Map with success flag and error message
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put(RESPONSE_SUCCESS, false);
        response.put(RESPONSE_MESSAGE, message);
        return response;
    }
} 