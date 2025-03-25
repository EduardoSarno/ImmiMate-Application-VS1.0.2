package co.immimate.auth.controller;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import co.immimate.auth.dto.LoginRequest;
import co.immimate.auth.dto.RegisterRequest;
import co.immimate.auth.service.AuthService;

/**
 * Controller for handling authentication-related endpoints
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(
    origins = {AuthController.LOCALHOST_ORIGIN}, 
    allowedHeaders = AuthController.ALL_HEADERS, 
    allowCredentials = AuthController.ALLOW_CREDENTIALS, 
    methods = {
        RequestMethod.GET, 
        RequestMethod.POST, 
        RequestMethod.PUT, 
        RequestMethod.DELETE, 
        RequestMethod.OPTIONS
    }
)
public class AuthController {
    
    // CORS configuration constants
    public static final String LOCALHOST_ORIGIN = "http://localhost:3000";
    public static final String ALL_HEADERS = "*";
    public static final String ALLOW_CREDENTIALS = "true";
    
    // API endpoint paths
    private static final String LOGIN_ENDPOINT = "/login";
    private static final String REGISTER_ENDPOINT = "/register";
    private static final String ME_ENDPOINT = "/me";
    private static final String LOGOUT_ENDPOINT = "/logout";
    private static final String VERIFY_ENDPOINT = "/verify";
    private static final String CSRF_TOKEN_ENDPOINT = "/csrf-token";

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Endpoint for user login
     * 
     * @param loginRequest Contains email and password
     * @param response HTTP response to add cookies
     * @return User details on successful authentication (token is in cookie)
     */
    @PostMapping(LOGIN_ENDPOINT)
    public ResponseEntity<?> loginUser(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response) {
        return authService.authenticateUser(loginRequest, response);
    }

    /**
     * Endpoint for user registration
     * 
     * @param registerRequest Contains user registration details
     * @return Success message on successful registration
     */
    @PostMapping(REGISTER_ENDPOINT)
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        return authService.registerUser(registerRequest);
    }

    /**
     * Get the current authenticated user
     * 
     * @return User details
     */
    @GetMapping(ME_ENDPOINT)
    public ResponseEntity<?> getCurrentUser() {
        return authService.getCurrentUser();
    }
    
    /**
     * Logout the current user by clearing the JWT cookie
     * 
     * @param response HTTP response to clear cookies
     * @return Success message
     */
    @PostMapping(LOGOUT_ENDPOINT)
    public ResponseEntity<?> logoutUser(HttpServletResponse response) {
        return authService.logoutUser(response);
    }
    
    /**
     * Verify if the current JWT token is valid
     * 
     * @return User details if valid, error if not
     */
    @GetMapping(VERIFY_ENDPOINT)
    public ResponseEntity<?> verifyToken() {
        return authService.verifyCurrentToken();
    }

    /**
     * Endpoint to get a CSRF token
     * This is needed by the frontend to make non-GET requests
     * 
     * @return Empty response with CSRF token in cookie
     */
    @GetMapping(CSRF_TOKEN_ENDPOINT)
    public ResponseEntity<?> getCsrfToken() {
        return ResponseEntity.ok().build();
    }
} 