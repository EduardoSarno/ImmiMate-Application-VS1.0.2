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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.immimate.profile.dto.ProfileSubmissionRequest;
import co.immimate.profile.dto.ProfileSubmissionResponse;
import co.immimate.profile.model.UserImmigrationProfile;
import co.immimate.profile.service.ProfileService;
import co.immimate.user.model.User;
import co.immimate.user.repository.UserRepository;

/**
 * REST controller for handling user immigration profile operations.
 */
@RestController
@RequestMapping(ProfileController.BASE_PATH)
public class ProfileController {
    // API path constants
    public static final String BASE_PATH = "/api/profiles";
    private static final String RECENT_PATH = "/recent";
    private static final String PROFILE_ID_PATH = "/{profileId}";
    private static final String DRAFT_PATH = "/draft";
    
    // OAuth2 attribute keys
    private static final String OAUTH2_ATTR_EMAIL = "email";
    private static final String OAUTH2_ATTR_SUB = "sub";
    
    // Response keys
    private static final String RESP_SUCCESS = "success";
    private static final String RESP_MESSAGE = "message";
    private static final String RESP_ERROR = "error";
    private static final String RESP_PROFILE_EXISTS = "profileExists";
    private static final String RESP_PROFILE_ID = "profileId";
    private static final String RESP_CREATED_AT = "createdAt";
    private static final String RESP_APPLICANT_NAME = "applicantName";
    private static final String RESP_FORM_DATA = "formData";
    
    // Success messages
    private static final String MSG_DRAFT_SAVED = "Form draft saved successfully";
    
    // Error messages
    private static final String ERR_USER_IDENTITY = "Could not determine user identity";
    private static final String ERR_RETRIEVE_PROFILE = "Failed to retrieve recent profile";
    private static final String ERR_INVALID_DATA = "Error saving form draft: Invalid data format";
    private static final String ERR_SAVING_DRAFT = "Error saving form draft: ";
    private static final String ERR_NO_DRAFT = "No draft found for this user";
    private static final String ERR_INVALID_DRAFT = "Error retrieving form draft: Invalid data format";
    private static final String ERR_RETRIEVE_DRAFT = "Error retrieving form draft: ";
    
    // Log messages
    private static final String LOG_SUBMISSION_REQUEST = "Received profile submission request for email: {}";
    private static final String LOG_PRINCIPAL_NAME = "Principal in request: {}";
    private static final String LOG_FULL_REQUEST = "FULL REQUEST OBJECT: {}";
    private static final String LOG_JOB_NOC = "Job Offer NOC Code received: {}";
    private static final String LOG_HAS_JOB = "Has Job Offer flag: {}";
    private static final String LOG_JOB_DETAILS = "All job offer fields - NOC Code: {}, LMIA Approved: {}, Wage: {}";
    private static final String LOG_AUTH_PRINCIPAL = "Authentication successful - Principal class: {}";
    private static final String LOG_EMAIL_OAUTH2 = "Setting user email from OAuth2 authentication: {}";
    private static final String LOG_GOOGLE_ID_OAUTH2 = "Setting Google ID from OAuth2 authentication: {}";
    private static final String LOG_EMAIL_STANDARD = "Setting user email from standard authentication: {}";
    private static final String LOG_NO_EMAIL = "Could not extract email from authentication principal";
    private static final String LOG_GOOGLE_ID_SET = "Google ID set in request: {}";
    private static final String LOG_REQUEST_DATA = "Request data: userEmail={}, userId={}, applicantName={}";
    private static final String LOG_SUBMISSION_SUCCESS = "Profile submission successful with ID: {}";
    private static final String LOG_SUBMISSION_FAILED = "Profile submission failed: {}";
    private static final String LOG_UNEXPECTED_ERROR = "Unexpected error during profile submission";
    private static final String LOG_GETTING_RECENT = "Getting recent profile for user: {}";
    private static final String LOG_EMAIL_FROM_OAUTH2 = "Extracted email from OAuth2 token: {}";
    private static final String LOG_GOOGLE_ID_FROM_OAUTH2 = "Extracted Google ID from OAuth2 token: {}";
    private static final String LOG_EMAIL_FROM_STANDARD = "Using email from standard authentication: {}";
    private static final String LOG_NO_EMAIL_OR_ID = "Could not extract email or Google ID from authentication principal";
    private static final String LOG_FOUND_BY_EMAIL = "Found profile by email: {}";
    private static final String LOG_NO_PROFILE_EMAIL = "No profile found for email: {}";
    private static final String LOG_FOUND_USER_BY_ID = "Found user by Google ID: {}";
    private static final String LOG_FOUND_PROFILE_ID = "Found profile for user with Google ID: {}";
    private static final String LOG_NO_USER_ID = "No user found with Google ID: {}";
    private static final String LOG_FOUND_RECENT = "Found recent profile with ID: {}";
    private static final String LOG_NO_RECENT = "No recent profile found for user: {}";
    private static final String LOG_GET_RECENT_ERROR = "Error getting recent profile: {}";
    private static final String LOG_DRAFT_SAVE = "Received form draft save request for user: {}";
    private static final String LOG_JSON_ERROR = "Error processing form data JSON";
    private static final String LOG_SAVE_ERROR = "Error saving form draft";
    private static final String LOG_DRAFT_RETRIEVAL = "Received form draft retrieval request for user: {}";
    private static final String LOG_PARSE_ERROR = "Error parsing form draft JSON data";
    private static final String LOG_PROCESS_ERROR = "Error processing form draft data";
    private static final String LOG_RUNTIME_ERROR = "Runtime error retrieving form draft";

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);
    
    private final ProfileService profileService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    
    @Autowired
    public ProfileController(ProfileService profileService, UserRepository userRepository) {
        this.profileService = profileService;
        this.objectMapper = new ObjectMapper();
        this.userRepository = userRepository;
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
        
        log.info(LOG_SUBMISSION_REQUEST, request.getUserEmail());
        log.info(LOG_PRINCIPAL_NAME, principal.getName());
        
        // Log the entire request object for debugging
        log.info(LOG_FULL_REQUEST, request);
        log.info(LOG_JOB_NOC, request.getJobOfferNocCode());
        log.info(LOG_HAS_JOB, request.getHasJobOffer());
        log.info(LOG_JOB_DETAILS, 
            request.getJobOfferNocCode(), request.getIsJobOfferLmiaApproved(), request.getJobOfferWageCAD());
        
        // Log authentication details for debugging
        log.info(LOG_AUTH_PRINCIPAL, principal.getClass().getName());
        
        try {
            // If no user email provided, extract from authentication
            if (request.getUserEmail() == null) {
                // Extract email based on authentication type
                String userEmail;
                String googleId = null;
                
                // Check if this is an OAuth2 authentication
                if (principal instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauth2Auth) {
                    
                    // Extract the email from the attributes
                    userEmail = oauth2Auth.getPrincipal().getAttribute(OAUTH2_ATTR_EMAIL);
                    // Also extract the Google ID (sub)
                    googleId = oauth2Auth.getPrincipal().getAttribute(OAUTH2_ATTR_SUB);
                    log.info(LOG_EMAIL_OAUTH2, userEmail);
                    log.info(LOG_GOOGLE_ID_OAUTH2, googleId);
                } else {
                    // Standard JWT authentication - use the name (which is the email)
                    userEmail = principal.getName();
                    log.info(LOG_EMAIL_STANDARD, userEmail);
                }
                
                if (userEmail != null) {
                    request.setUserEmail(userEmail);
                } else {
                    log.warn(LOG_NO_EMAIL);
                }
                
                if (googleId != null) {
                    request.setGoogleId(googleId);
                    log.info(LOG_GOOGLE_ID_SET, googleId);
                }
            }
            
            // Log the received data for debugging
            log.info(LOG_REQUEST_DATA,
                    request.getUserEmail(), request.getUserId(), request.getApplicantName());
            
            ProfileSubmissionResponse response = profileService.submitProfile(request);
            
            if (response.isSuccess()) {
                log.info(LOG_SUBMISSION_SUCCESS, response.getProfileId());
                return ResponseEntity.ok(response);
            } else {
                log.error(LOG_SUBMISSION_FAILED, response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error(LOG_UNEXPECTED_ERROR, e);
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
    @GetMapping(RECENT_PATH)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getRecentProfile(Principal principal) {
        log.info(LOG_GETTING_RECENT, principal.getName());
        
        try {
            // Extract the user's email - handle both OAuth2 and JWT authentication
            String userEmail;
            String googleId = null;
            
            // Check if this is an OAuth2 authentication
            if (principal instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauth2Auth) {
                
                // Extract the email from the attributes
                userEmail = oauth2Auth.getPrincipal().getAttribute(OAUTH2_ATTR_EMAIL);
                // Also extract the Google ID (sub)
                googleId = oauth2Auth.getPrincipal().getAttribute(OAUTH2_ATTR_SUB);
                log.info(LOG_EMAIL_FROM_OAUTH2, userEmail);
                log.info(LOG_GOOGLE_ID_FROM_OAUTH2, googleId);
            } else {
                // Standard JWT authentication - use the name (which is the email)
                userEmail = principal.getName();
                log.info(LOG_EMAIL_FROM_STANDARD, userEmail);
            }
            
            if (userEmail == null && googleId == null) {
                log.warn(LOG_NO_EMAIL_OR_ID);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(RESP_ERROR, ERR_USER_IDENTITY));
            }
            
            // Find the user's most recent profile
            Optional<UserImmigrationProfile> recentProfile;
            
            // Try by email first
            if (userEmail != null) {
                recentProfile = profileService.getMostRecentProfileByEmail(userEmail);
                if (recentProfile.isPresent()) {
                    log.info(LOG_FOUND_BY_EMAIL, userEmail);
                } else {
                    log.info(LOG_NO_PROFILE_EMAIL, userEmail);
                }
            } else {
                recentProfile = Optional.empty();
            }
            
            // If not found and we have a Google ID, try to find user by Google ID
            if (recentProfile.isEmpty() && googleId != null) {
                // Get user first
                Optional<User> userOpt = userRepository.findByGoogleId(googleId);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    log.info(LOG_FOUND_USER_BY_ID, googleId);
                    
                    // Then get profile by user
                    recentProfile = profileService.getMostRecentProfileByUser(user);
                    if (recentProfile.isPresent()) {
                        log.info(LOG_FOUND_PROFILE_ID, googleId);
                    }
                } else {
                    log.info(LOG_NO_USER_ID, googleId);
                }
            }
            
            if (recentProfile.isPresent()) {
                UserImmigrationProfile profile = recentProfile.get();
                log.info(LOG_FOUND_RECENT, profile.getApplicationId());
                
                // Return a simplified response with just the key information
                Map<String, Object> response = new HashMap<>();
                response.put(RESP_PROFILE_EXISTS, true);
                response.put(RESP_PROFILE_ID, profile.getApplicationId());
                response.put(RESP_CREATED_AT, profile.getCreatedAt());
                response.put(RESP_APPLICANT_NAME, profile.getApplicantName());
                return ResponseEntity.ok(response);
            } else {
                log.info(LOG_NO_RECENT, userEmail);
                return ResponseEntity.ok(Map.of(RESP_PROFILE_EXISTS, false));
            }
        } catch (Exception e) {
            log.error(LOG_GET_RECENT_ERROR, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(RESP_ERROR, ERR_RETRIEVE_PROFILE));
        }
    }
    
    /**
     * Get a specific profile by ID
     * 
     * @param profileId The profile ID
     * @param principal The authenticated user
     * @return The requested profile if found and user has access
     */
    @GetMapping(PROFILE_ID_PATH)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getProfileById(@PathVariable UUID profileId, Principal principal) {
        // This endpoint will be implemented later
        // For now, return a simple message
        return ResponseEntity.ok("This endpoint is under construction. Requested profile ID: " + profileId);
    }
    
    /**
     * Save form draft to the server
     * 
     * @param formData The form data as a JSON object
     * @param principal The authenticated user
     * @return Response with success status
     */
    @PostMapping(DRAFT_PATH)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> saveFormDraft(
            @RequestBody Map<String, Object> formData,
            Principal principal) {
        
        log.info(LOG_DRAFT_SAVE, principal.getName());
        
        try {
            // Convert form data to JSON string for storage
            String formDataJson = objectMapper.writeValueAsString(formData);
            
            // Extract email and googleId from authentication - handle both OAuth2 and standard auth
            String userEmail;
            String googleId = null;
            
            // Check if this is an OAuth2 authentication
            if (principal instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauth2Auth) {
                // Extract the email from the attributes
                userEmail = oauth2Auth.getPrincipal().getAttribute(OAUTH2_ATTR_EMAIL);
                // Also extract the Google ID (sub)
                googleId = oauth2Auth.getPrincipal().getAttribute(OAUTH2_ATTR_SUB);
                log.info("Using OAuth2 email for draft save: {}, Google ID: {}", userEmail, googleId);
            } else {
                // Standard authentication - use principal name
                userEmail = principal.getName();
                log.info("Using standard auth email for draft save: {}", userEmail);
            }
            
            if (userEmail == null && googleId == null) {
                log.error("Could not determine user identity for form draft save");
                
                Map<String, Object> response = new HashMap<>();
                response.put(RESP_SUCCESS, false);
                response.put(RESP_MESSAGE, "Could not determine user identity");
                
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
            // Find the user by email or Google ID and save the draft
            try {
                // Store the form draft in the profile service - now accepting googleId parameter
                profileService.saveProfileDraftWithGoogleId(userEmail, googleId, formDataJson);
                
                Map<String, Object> response = new HashMap<>();
                response.put(RESP_SUCCESS, true);
                response.put(RESP_MESSAGE, MSG_DRAFT_SAVED);
                
                return ResponseEntity.ok(response);
            } catch (IllegalArgumentException e) {
                log.error("User not found when trying to save draft: {}", e.getMessage());
                
                Map<String, Object> response = new HashMap<>();
                response.put(RESP_SUCCESS, false);
                response.put(RESP_MESSAGE, "User not found: " + e.getMessage());
                
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (JsonProcessingException e) {
            log.error(LOG_JSON_ERROR, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put(RESP_SUCCESS, false);
            response.put(RESP_MESSAGE, ERR_INVALID_DATA);
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error(LOG_SAVE_ERROR, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put(RESP_SUCCESS, false);
            response.put(RESP_MESSAGE, ERR_SAVING_DRAFT + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Retrieve the latest form draft for the current user
     * 
     * @param principal The authenticated user
     * @return Response with the form draft data
     */
    @GetMapping(DRAFT_PATH)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getFormDraft(Principal principal) {
        log.info(LOG_DRAFT_RETRIEVAL, principal.getName());
        
        try {
            // Extract email and googleId from authentication - handle both OAuth2 and standard auth
            String userEmail;
            String googleId = null;
            
            // Check if this is an OAuth2 authentication
            if (principal instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauth2Auth) {
                // Extract the email from the attributes
                userEmail = oauth2Auth.getPrincipal().getAttribute(OAUTH2_ATTR_EMAIL);
                // Also extract the Google ID (sub)
                googleId = oauth2Auth.getPrincipal().getAttribute(OAUTH2_ATTR_SUB);
                log.info("Using OAuth2 email for draft retrieval: {}, Google ID: {}", userEmail, googleId);
            } else {
                // Standard authentication - use principal name
                userEmail = principal.getName();
                log.info("Using standard auth email for draft retrieval: {}", userEmail);
            }
            
            if (userEmail == null && googleId == null) {
                log.error("Could not determine user identity for form draft retrieval");
                
                Map<String, Object> response = new HashMap<>();
                response.put(RESP_SUCCESS, false);
                response.put(RESP_MESSAGE, "Could not determine user identity");
                
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
            // Get the latest form draft from the profile service - now accepting googleId parameter
            Optional<String> formDraftJson = profileService.getLatestProfileDraftWithGoogleId(userEmail, googleId);
            
            if (formDraftJson.isPresent()) {
                // Parse the JSON string back to a Map
                Map<String, Object> formData = objectMapper.readValue(
                    formDraftJson.get(), 
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)
                );
                
                Map<String, Object> response = new HashMap<>();
                response.put(RESP_SUCCESS, true);
                response.put(RESP_FORM_DATA, formData);
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put(RESP_SUCCESS, false);
                response.put(RESP_MESSAGE, ERR_NO_DRAFT);
                
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (JsonProcessingException e) {
            log.error(LOG_PARSE_ERROR, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put(RESP_SUCCESS, false);
            response.put(RESP_MESSAGE, ERR_INVALID_DRAFT);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error(LOG_PROCESS_ERROR, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put(RESP_SUCCESS, false);
            response.put(RESP_MESSAGE, ERR_RETRIEVE_DRAFT + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (RuntimeException e) {
            log.error(LOG_RUNTIME_ERROR, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put(RESP_SUCCESS, false);
            response.put(RESP_MESSAGE, ERR_RETRIEVE_DRAFT + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
} 