package co.immimate.profile.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.immimate.profile.dto.ProfileSubmissionRequest;
import co.immimate.profile.dto.ProfileSubmissionResponse;
import co.immimate.profile.model.UserImmigrationProfile;
import co.immimate.profile.service.ProfileService;

/**
 * REST controller for handling user immigration profile operations.
 */
@RestController
@RequestMapping("/profiles")  // Removed /api prefix since the app already has /api context
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);
    
    private final ProfileService profileService;
    
    @Autowired
    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }
    
    /**
     * Submit a new immigration profile
     * 
     * @param request The profile submission request
     * @param principal The authenticated user
     * @return Response with the profile ID and status
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileSubmissionResponse> submitProfile(
            @Valid @RequestBody ProfileSubmissionRequest request,
            Principal principal) {
        
        log.info("Received profile submission request for email: {}", request.getUserEmail());
        log.info("Principal in request: {}", principal != null ? principal.getName() : "null");
        
        // Log authentication details for debugging
        if (principal != null) {
            log.info("Authentication successful - Principal class: {}", principal.getClass().getName());
        } else {
            log.warn("No principal found - this should not happen with @PreAuthorize");
        }
        
        try {
            // If no user email provided, extract from authentication
            if (request.getUserEmail() == null && principal != null) {
                // Extract email based on authentication type
                String userEmail;
                
                // Check if this is an OAuth2 authentication
                if (principal instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) {
                    // This is an OAuth2 authentication (Google, etc.)
                    org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauth2Auth = 
                        (org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) principal;
                    
                    // Extract the email from the attributes
                    userEmail = oauth2Auth.getPrincipal().getAttribute("email");
                    log.info("Setting user email from OAuth2 authentication: {}", userEmail);
                } else {
                    // Standard JWT authentication - use the name (which is the email)
                    userEmail = principal.getName();
                    log.info("Setting user email from standard authentication: {}", userEmail);
                }
                
                if (userEmail != null) {
                    request.setUserEmail(userEmail);
                } else {
                    log.warn("Could not extract email from authentication principal");
                }
            }
            
            // Log the received data for debugging
            log.info("Request data: userEmail={}, userId={}, applicantName={}",
                    request.getUserEmail(), request.getUserId(), request.getApplicantName());
            
            ProfileSubmissionResponse response = profileService.submitProfile(request);
            
            if (response.isSuccess()) {
                log.info("Profile submission successful with ID: {}", response.getProfileId());
                return ResponseEntity.ok(response);
            } else {
                log.error("Profile submission failed: {}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("Unexpected error during profile submission", e);
            ProfileSubmissionResponse errorResponse = ProfileSubmissionResponse.error(
                    "Server error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Get the most recent profile for the authenticated user
     * 
     * @param principal The authenticated user
     * @return The user's most recent profile
     */
    @GetMapping("/recent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getRecentProfile(Principal principal) {
        log.info("Getting recent profile for user: {}", principal.getName());
        
        try {
            // Extract the user's email - handle both OAuth2 and JWT authentication
            String userEmail;
            
            // Check if this is an OAuth2 authentication
            if (principal instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) {
                // This is an OAuth2 authentication (Google, etc.)
                org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauth2Auth = 
                    (org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) principal;
                
                // Extract the email from the attributes
                userEmail = oauth2Auth.getPrincipal().getAttribute("email");
                log.info("Extracted email from OAuth2 token: {}", userEmail);
            } else {
                // Standard JWT authentication - use the name (which is the email)
                userEmail = principal.getName();
                log.info("Using email from standard authentication: {}", userEmail);
            }
            
            if (userEmail == null) {
                log.warn("Could not extract email from authentication principal");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Could not determine user email"));
            }
            
            // Find the user's most recent profile
            Optional<UserImmigrationProfile> recentProfile = profileService.getMostRecentProfileByEmail(userEmail);
            
            if (recentProfile.isPresent()) {
                UserImmigrationProfile profile = recentProfile.get();
                log.info("Found recent profile with ID: {}", profile.getApplicationId());
                
                // Return a simplified response with just the key information
                Map<String, Object> response = new HashMap<>();
                response.put("profileExists", true);
                response.put("profileId", profile.getApplicationId());
                response.put("createdAt", profile.getCreatedAt());
                response.put("applicantName", profile.getApplicantName());
                return ResponseEntity.ok(response);
            } else {
                log.info("No recent profile found for user: {}", userEmail);
                return ResponseEntity.ok(Map.of("profileExists", false));
            }
        } catch (Exception e) {
            log.error("Error getting recent profile: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve recent profile"));
        }
    }
    
    /**
     * Get a specific profile by ID
     * 
     * @param profileId The profile ID
     * @param principal The authenticated user
     * @return The requested profile if found and user has access
     */
    @GetMapping("/{profileId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getProfileById(@PathVariable UUID profileId, Principal principal) {
        // This endpoint will be implemented later
        // For now, return a simple message
        return ResponseEntity.ok("This endpoint is under construction. Requested profile ID: " + profileId);
    }
} 