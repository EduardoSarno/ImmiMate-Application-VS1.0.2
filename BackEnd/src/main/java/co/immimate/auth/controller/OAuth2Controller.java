package co.immimate.auth.controller;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import co.immimate.auth.security.JwtUtil;
import co.immimate.profile.repository.UserImmigrationProfileRepository;
import co.immimate.user.model.User;
import co.immimate.user.repository.UserRepository;

@RestController
@RequestMapping("/oauth2")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class OAuth2Controller {
    private static final Logger logger = LoggerFactory.getLogger(OAuth2Controller.class);

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private UserImmigrationProfileRepository profileRepository;

    /**
     * This endpoint is for OAuth2 success redirection
     * It's called when OAuth2 login is successful
     */
    @GetMapping("/login/success")
    public RedirectView loginSuccess() {
        logger.info("OAuth2 login success endpoint called");
        
        // Get the authenticated user from the security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.info("Authentication type: " + authentication.getClass().getName());
        
        // Check if this is an OAuth2 authentication
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oauth2User = oauthToken.getPrincipal();
            Map<String, Object> attributes = oauth2User.getAttributes();
            
            // Log the OAuth2 attributes
            logger.info("OAuth2 attributes:");
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                logger.info("OAuth2 attribute: " + entry.getKey() + " = " + entry.getValue());
            }
            
            // Extract email and name
            String email = (String) attributes.get("email");
            String name = (String) attributes.get("name");
            String providerId = (String) attributes.get("sub");
            
            logger.info("OAuth2 login: email={}, name={}, provider_id={}", email, name, providerId);
            
            // Find or create the user
            Optional<User> existingUser = userRepository.findByEmail(email);
            User user;
            
            if (existingUser.isPresent()) {
                user = existingUser.get();
                logger.info("User found: {}", user.getId());
            } else {
                // Create a new user record
                user = new User();
                user.setEmail(email);
                user.setName(name);
                user.setGoogleId(providerId);
                user.setRole("USER");
                user = userRepository.save(user);
                logger.info("New user created: {}", user.getId());
            }
            
            // Generate a JWT token
            String jwtToken = jwtUtil.generateToken(user);
            
            // Create the redirect URL with token and user info
            String redirectUrl = "http://localhost:3000/oauth/callback?token=" + jwtToken + 
                                 "&email=" + email + 
                                 "&userId=" + user.getId();
            
            logger.info("OAuth2 login successful, path set to: {}", redirectUrl);
            logger.info("UPDATED CODE - Redirecting to React frontend: {}", redirectUrl);
            
            return new RedirectView(redirectUrl);
        }
        
        // If not OAuth2 authentication, redirect to the login page
        logger.error("Not an OAuth2 authentication");
        return new RedirectView("http://localhost:3000/login?error=authentication_required");
    }
} 