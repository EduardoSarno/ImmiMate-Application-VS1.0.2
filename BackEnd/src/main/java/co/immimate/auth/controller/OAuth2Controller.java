package co.immimate.auth.controller;

import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import co.immimate.auth.security.JwtUtil;
import co.immimate.user.model.User;
import co.immimate.user.repository.UserRepository;

/**
 * Controller for handling OAuth2 authentication flows
 */
@RestController
@RequestMapping("/api/oauth2")
@CrossOrigin(
    origins = {OAuth2Controller.LOCALHOST_ORIGIN}, 
    allowedHeaders = OAuth2Controller.ALL_HEADERS, 
    allowCredentials = OAuth2Controller.ALLOW_CREDENTIALS, 
    methods = {
        RequestMethod.GET, 
        RequestMethod.POST, 
        RequestMethod.PUT, 
        RequestMethod.DELETE, 
        RequestMethod.OPTIONS
    }
)
public class OAuth2Controller {
    // Logger
    private static final Logger logger = LoggerFactory.getLogger(OAuth2Controller.class);
    
    // CORS configuration constants
    public static final String LOCALHOST_ORIGIN = "http://localhost:3000";
    public static final String ALL_HEADERS = "*";
    public static final String ALLOW_CREDENTIALS = "true";
    
    // OAuth2 attribute keys
    private static final String ATTR_EMAIL = "email";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_SUB = "sub";
    
    // User role
    private static final String ROLE_USER = "USER";
    
    // URL parameters
    private static final String PARAM_EMAIL = "email";
    private static final String PARAM_USER_ID = "userId";
    private static final String PARAM_NEW_USER = "newUser";
    private static final String PARAM_NEW_USER_VALUE = "true";
    private static final String PARAM_ERROR = "error";
    private static final String ERROR_AUTH_REQUIRED = "authentication_required";
    
    // Endpoints
    private static final String LOGIN_SUCCESS_ENDPOINT = "/login/success";
    
    // Frontend URLs
    private static final String FRONTEND_BASE_URL = "http://localhost:3000";
    private static final String OAUTH_CALLBACK_URL = FRONTEND_BASE_URL + "/oauth/callback";
    private static final String LOGIN_URL = FRONTEND_BASE_URL + "/login";
    
    // Log messages
    private static final String LOG_OAUTH_SUCCESS_ENDPOINT = "OAuth2 login success endpoint called";
    private static final String LOG_AUTH_TYPE = "Authentication type: {}";
    private static final String LOG_OAUTH_ATTRIBUTES = "OAuth2 attributes:";
    private static final String LOG_OAUTH_ATTRIBUTE = "OAuth2 attribute: {} = {}";
    private static final String LOG_OAUTH_LOGIN = "OAuth2 login: email={}, name={}, provider_id={}";
    private static final String LOG_USER_FOUND = "User found: {}";
    private static final String LOG_UPDATING_GOOGLE_ID = "Updating Google ID for user {} from {} to {}";
    private static final String LOG_NEW_USER_CREATED = "New user created: {}";
    private static final String LOG_JWT_COOKIE_ADDED = "JWT cookie added to response";
    private static final String LOG_NEW_USER_PARAM = "New user created, adding newUser=true parameter to redirect";
    private static final String LOG_OAUTH_REDIRECT = "OAuth2 login successful, redirecting to: {}";
    private static final String LOG_NOT_OAUTH = "Not an OAuth2 authentication";

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * This endpoint is for OAuth2 success redirection
     * It's called when OAuth2 login is successful
     * It sets the JWT token in a cookie and redirects to the frontend
     */
    @GetMapping(LOGIN_SUCCESS_ENDPOINT)
    public RedirectView loginSuccess(HttpServletResponse response) {
        logger.info(LOG_OAUTH_SUCCESS_ENDPOINT);
        
        // Get the authenticated user from the security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.info(LOG_AUTH_TYPE, authentication.getClass().getName());
        
        // Check if this is an OAuth2 authentication
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2User oauth2User = oauthToken.getPrincipal();
            Map<String, Object> attributes = oauth2User.getAttributes();
            
            // Log the OAuth2 attributes
            logger.info(LOG_OAUTH_ATTRIBUTES);
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                logger.info(LOG_OAUTH_ATTRIBUTE, entry.getKey(), entry.getValue());
            }
            
            // Extract email and name
            String email = (String) attributes.get(ATTR_EMAIL);
            String name = (String) attributes.get(ATTR_NAME);
            String providerId = (String) attributes.get(ATTR_SUB);
            
            logger.info(LOG_OAUTH_LOGIN, email, name, providerId);
            
            // Find or create the user
            Optional<User> existingUser = userRepository.findByEmail(email);
            User user;
            
            if (existingUser.isPresent()) {
                user = existingUser.get();
                logger.info(LOG_USER_FOUND, user.getId());
                
                // Update the Google ID if it's different
                if (providerId != null && !providerId.equals(user.getGoogleId())) {
                    logger.info(LOG_UPDATING_GOOGLE_ID, 
                        user.getId(), user.getGoogleId(), providerId);
                    user.setGoogleId(providerId);
                    user = userRepository.save(user);
                }
            } else {
                // Create a new user record
                user = new User();
                user.setEmail(email);
                user.setName(name);
                user.setGoogleId(providerId);
                user.setRole(ROLE_USER);
                user = userRepository.save(user);
                logger.info(LOG_NEW_USER_CREATED, user.getId());
            }
            
            // Add JWT token as HttpOnly cookie
            jwtUtil.addJwtCookieToResponse(response, user);
            logger.info(LOG_JWT_COOKIE_ADDED);
            
            // Create the redirect URL with only user info (no token)
            StringBuilder redirectUrlBuilder = new StringBuilder(OAUTH_CALLBACK_URL)
                .append("?").append(PARAM_EMAIL).append("=").append(email)
                .append("&").append(PARAM_USER_ID).append("=").append(user.getId());
            
            // Add a parameter to indicate if this was a new user registration
            if (existingUser.isEmpty()) {
                redirectUrlBuilder.append("&").append(PARAM_NEW_USER).append("=").append(PARAM_NEW_USER_VALUE);
                logger.info(LOG_NEW_USER_PARAM);
            }
            
            String redirectUrl = redirectUrlBuilder.toString();
            logger.info(LOG_OAUTH_REDIRECT, redirectUrl);
            
            return new RedirectView(redirectUrl);
        }
        
        // If not OAuth2 authentication, redirect to the login page
        logger.error(LOG_NOT_OAUTH);
        return new RedirectView(LOGIN_URL + "?" + PARAM_ERROR + "=" + ERROR_AUTH_REQUIRED);
    }
} 