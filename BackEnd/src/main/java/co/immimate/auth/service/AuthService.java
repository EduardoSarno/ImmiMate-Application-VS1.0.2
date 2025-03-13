package co.immimate.auth.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import co.immimate.auth.dto.AuthResponse;
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

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

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
     * @return JWT token and user details on successful authentication
     */
    public ResponseEntity<?> authenticateUser(LoginRequest loginRequest) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Get user from database
            Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
            }

            User user = userOpt.get();

            // Generate JWT token
            String jwt = jwtUtil.generateToken(user);

            // Return token and user details
            return ResponseEntity.ok(new AuthResponse(
                    jwt,
                    user.getEmail(),
                    user.getId().toString(),
                    user.getRole()
            ));
        } catch (BadCredentialsException e) {
            logger.error("Invalid credentials: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        } catch (Exception e) {
            logger.error("Authentication error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Authentication failed");
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
                Map<String, String> response = new HashMap<>();
                response.put("message", "Either email or phone number must be provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Check if email already exists (if email is provided)
            if (registerRequest.getEmail() != null && !registerRequest.getEmail().trim().isEmpty()) {
                if (userRepository.existsByEmail(registerRequest.getEmail())) {
                    Map<String, String> response = new HashMap<>();
                    response.put("message", "Email is already in use");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
            }
            
            // Create new user
            User user = new User();
            
            // Set required fields
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            user.setRole("USER"); // Default role
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

            Map<String, String> response = new HashMap<>();
            response.put("message", "User registered successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Registration error: {}", e.getMessage(), e);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Registration failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Gets the currently authenticated user
     *
     * @return User details
     */
    public ResponseEntity<?> getCurrentUser() {
        try {
            // Get current authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
            }

            // Get user email from authentication principal
            String email = authentication.getName();
            
            // Find user in database
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            User user = userOpt.get();

            // Return user details (excluding sensitive information)
            Map<String, Object> userDetails = new HashMap<>();
            userDetails.put("id", user.getId().toString());
            userDetails.put("email", user.getEmail());
            userDetails.put("firstName", user.getFirstName());
            userDetails.put("lastName", user.getLastName());
            userDetails.put("name", user.getName());
            userDetails.put("role", user.getRole());

            return ResponseEntity.ok(userDetails);
        } catch (Exception e) {
            logger.error("Error getting current user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving user details");
        }
    }
} 