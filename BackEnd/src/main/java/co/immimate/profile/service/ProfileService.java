package co.immimate.profile.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.immimate.profile.dto.ProfileSubmissionRequest;
import co.immimate.profile.dto.ProfileSubmissionResponse;
import co.immimate.profile.model.JobsNoc;
import co.immimate.profile.model.ProfileDraft;
import co.immimate.profile.model.UserImmigrationProfile;
import co.immimate.profile.repository.JobsNocRepository;
import co.immimate.profile.repository.ProfileDraftRepository;
import co.immimate.profile.repository.UserImmigrationProfileRepository;
import co.immimate.user.model.User;
import co.immimate.user.repository.UserRepository;

/**
 * Service class for managing user immigration profiles.
 * Provides methods to interact with profile data.
 */
@Service
public class ProfileService {
    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);
    
    // Marital status constants
    private static final String MARITAL_STATUS_SINGLE = "Single";
    
    
    // Score validation limits
    private static final int MIN_LANGUAGE_SCORE = 1;
    private static final int MAX_LANGUAGE_SCORE = 12;
    private static final int MAX_REASONABLE_AGE = 120;
    
    // Common validation messages
    private static final String VALIDATION_REQUIRED_FIELD = "%s is required";
    private static final String VALIDATION_SCORE_RANGE = "%s score must be between %d and %d";
    private static final String VALIDATION_NON_NEGATIVE = "%s must be non-negative";
    private static final String VALIDATION_POSITIVE = "%s must be positive";
    
    // Log messages
    private static final String LOG_FETCHING_PROFILE = "Fetching most recent profile for user email: %s";
    private static final String LOG_FETCHING_PROFILE_BY_ID = "Fetching most recent profile for user: %s";
    private static final String LOG_PROCESSING_SUBMISSION = "Processing profile submission for user email: %s";
    private static final String LOG_JOB_OFFER_NOC = "CRITICAL DEBUG - Job offer NOC code in incoming request: %s";
    private static final String LOG_VALIDATION_ERROR = "Validation error in profile submission: %s";
    private static final String LOG_USER_NOT_FOUND = "User not found for ID: %s or Email: %s";
    private static final String LOG_USER_FOUND = "Found user: ID=%s, Email=%s";
    private static final String LOG_PROFILE_CREATED = "Created profile object in memory for user: %s";
    private static final String LOG_PROFILE_SAVED = "Saved profile with ID: %s for user: %s";
    private static final String LOG_DB_ERROR = "Database error while saving profile: %s";
    private static final String LOG_UNEXPECTED_ERROR = "Unexpected error in profile submission process: %s";
    private static final String LOG_USER_BY_GOOGLE_ID = "Found user by Google ID: %s";
    private static final String LOG_NEW_USER_CREATED = "Creating new user with email: %s";
    private static final String LOG_PROFILE_START = "=================== START OF createProfileFromRequest ===================";
    private static final String LOG_PROFILE_END = "=================== END OF createProfileFromRequest ===================";
    private static final String LOG_SETTING_UUID = "Setting new UUID for profile: %s";
    private static final String LOG_SETTING_USER = "Setting user on profile: userId=%s";
    private static final String LOG_BASIC_INFO_SET = "Set basic profile information for: %s";
    private static final String LOG_EDUCATION_INFO_SET = "Set education information with level: %s";
    private static final String LOG_LANGUAGE_INFO_SET = "Set language information with primary test type: %s";
    private static final String LOG_SAVING_DRAFT = "Saving profile draft for user: %s";
    private static final String LOG_RETRIEVING_DRAFT = "Retrieving latest profile draft for user: %s";
    private static final String LOG_DELETING_DRAFTS = "Deleting all profile drafts for user: %s";
    
    private final UserImmigrationProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final JobsNocRepository jobsNocRepository;
    private final ProfileDraftRepository profileDraftRepository;
    
    
    /**
     * Constructor for ProfileService.
     * 
     * @param profileRepository Repository for accessing profile data
     * @param userRepository Repository for accessing user data
     * @param jobsNocRepository Repository for accessing NOC code data
     * @param profileDraftRepository Repository for accessing form drafts
     */
    @Autowired
    public ProfileService(
            UserImmigrationProfileRepository profileRepository, 
            UserRepository userRepository,
            JobsNocRepository jobsNocRepository,
            ProfileDraftRepository profileDraftRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.jobsNocRepository = jobsNocRepository;
        this.profileDraftRepository = profileDraftRepository;
    }
    
    /**
     * Retrieves a user immigration profile by its application ID.
     * 
     * @param applicationId The UUID of the profile to retrieve
     * @return An Optional containing the profile if found, or empty if not found
     */
    public Optional<UserImmigrationProfile> getProfileByApplicationId(UUID applicationId) {
        Optional<UserImmigrationProfile> profileOpt = profileRepository.findByApplicationId(applicationId);
        
        // Log profile data if found
        profileOpt.ifPresent(profile -> {
            log.debug("Retrieved profile -> UserImmigrationProfile(tradesCertification={}, hasProvincialNomination={})", 
                    profile.getTradesCertification(), profile.isHasProvincialNomination());
            log.debug("trades_certification = {}", profile.getTradesCertification());
            log.debug("has_provincial_nomination = {}", profile.isHasProvincialNomination());
            
            // Log other wrapper types to verify data types
            log.debug("secondaryTestSpeakingScore = {}", profile.getSecondaryTestSpeakingScore());
            log.debug("isJobOfferLmiaApproved = {}", profile.getIsJobOfferLmiaApproved());
            log.debug("nocCodeCanadian = {}", profile.getNocCodeCanadian());
            log.debug("educationCompletedInCanada = {}", profile.getEducationCompletedInCanada());
        });
        
        return profileOpt;
    }

    /**
     * Retrieves the most recent profile for a user by email.
     * 
     * @param email The email of the user
     * @return An Optional containing the most recent profile if found, or empty if not found
     */
    public Optional<UserImmigrationProfile> getMostRecentProfileByEmail(String email) {
        log.info(LOG_FETCHING_PROFILE, email);
        return profileRepository.findFirstByUserEmailOrderByCreatedAtDesc(email);
    }

    /**
     * Retrieves the most recent profile for a specific user.
     * 
     * @param user The User entity
     * @return An Optional containing the most recent profile if found, or empty if not found
     */
    public Optional<UserImmigrationProfile> getMostRecentProfileByUser(User user) {
        log.info(LOG_FETCHING_PROFILE_BY_ID, user.getId());
        return profileRepository.findFirstByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Submits a new user immigration profile for evaluation.
     * Creates a new profile record in the database based on the provided request data.
     * 
     * @param request The profile submission request containing all necessary profile data
     * @return A response object containing success/error status and message
     */
    @Transactional
    public ProfileSubmissionResponse submitProfile(ProfileSubmissionRequest request) {
        log.info(LOG_PROCESSING_SUBMISSION, request.getUserEmail());
        
        // CRITICAL: Directly log and capture the job offer NOC code from the incoming request
        Integer jobOfferNocCodeFromRequest = request.getJobOfferNocCode();
        log.info(LOG_JOB_OFFER_NOC, jobOfferNocCodeFromRequest);
        
        try {
            // Basic validation
            Optional<String> validationError = validateProfileSubmission(request);
            if (validationError.isPresent()) {
                String errorMessage = validationError.get();
                log.warn(LOG_VALIDATION_ERROR, errorMessage);
                // Ensure the full validation error message is included in the response
                return ProfileSubmissionResponse.error(errorMessage);
            }
            
            // Find or create user
            User user = findUserByIdOrEmail(request);
            if (user == null) {
                log.error(LOG_USER_NOT_FOUND, request.getUserId(), request.getUserEmail());
                return ProfileSubmissionResponse.error("User not found. Please ensure you're logged in or your email is registered.");
            }
            
            log.info(LOG_USER_FOUND, user.getId(), user.getEmail());
            
            // Create profile entity from request
            UserImmigrationProfile profile;
            try {
                profile = createProfileFromRequest(request, user);
                log.info(LOG_PROFILE_CREATED, user.getId());
            } catch (Exception e) {
                log.error("Error creating profile from request: {}", e.getMessage(), e);
                return ProfileSubmissionResponse.error("Error creating profile: " + e.getMessage());
            }
            
            // Save the profile - Wrap this in a separate try-catch to isolate errors
            try {
                // With the @Type annotation, Hibernate will handle the JSONB conversion automatically
                profile = profileRepository.save(profile);
                log.info(LOG_PROFILE_SAVED, profile.getApplicationId(), user.getId());
            } catch (RuntimeException e) {
                // Log the original exception details for debugging
                log.error(LOG_DB_ERROR, e.getMessage(), e);
                
                // Extract useful information from PostgreSQL exceptions
                String errorMessage = "Error saving profile to database";
                Throwable rootCause = getRootCause(e);
                
                // Check for common database errors and provide more specific messages
                if (rootCause != null && rootCause.getMessage() != null) {
                    String rootMsg = rootCause.getMessage();
                    
                    if (rootMsg.contains("operator does not exist: text = integer") || 
                        rootMsg.contains("Bad value for type int")) {
                        errorMessage = "Type mismatch error: NOC codes must be valid integers. Please check that all NOC codes are numbers without any letters or special characters.";
                    } else if (rootMsg.contains("value too long")) {
                        errorMessage = "Data exceeds maximum allowed length for some fields";
                    } else if (rootMsg.contains("violates not-null constraint")) {
                        errorMessage = "Missing required field in profile data";
                    } else if (rootMsg.contains("duplicate key")) {
                        errorMessage = "Profile with this information already exists";
                    }
                }
                
                return ProfileSubmissionResponse.error(errorMessage + ". Please check your submission and try again.");
            }
            
            // Return success response
            return ProfileSubmissionResponse.success(
                profile.getApplicationId(),
                "Profile submitted successfully"
            );
        } catch (Exception e) {
            log.error(LOG_UNEXPECTED_ERROR, e.getMessage(), e);
            return ProfileSubmissionResponse.error("Unexpected error in profile submission process: " + e.getMessage());
        }
    }
    
    /**
     * Gets the root cause of an exception
     * 
     * @param throwable The exception to get the root cause of
     * @return The root cause, or the original exception if no root cause is found
     */
    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable.getCause();
        if (cause == null) {
            return throwable;
        }
        
        // Follow the chain of causes
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        
        return cause;
    }
    
    /**
     * Find user by ID or email
     */
    private User findUserByIdOrEmail(ProfileSubmissionRequest request) {
        // First try by ID if provided
        if (request.getUserId() != null) {
            Optional<User> userById = userRepository.findById(request.getUserId());
            if (userById.isPresent()) {
                return userById.get();
            }
        }
        
        // Then try by email
        if (request.getUserEmail() != null) {
            Optional<User> userByEmail = userRepository.findByEmail(request.getUserEmail());
            if (userByEmail.isPresent()) {
                return userByEmail.get();
            }
        }
        
        // Try by Google ID if provided in the request
        if (request.getGoogleId() != null) {
            Optional<User> userByGoogleId = userRepository.findByGoogleId(request.getGoogleId());
            if (userByGoogleId.isPresent()) {
                log.info(LOG_USER_BY_GOOGLE_ID, request.getGoogleId());
                return userByGoogleId.get();
            }
        }
        
        // If we still haven't found a user but have an email, create a new one
        if (request.getUserEmail() != null) {
            // Create a new user if none found with the given email
            User newUser = new User();
            newUser.setEmail(request.getUserEmail());
            newUser.setId(UUID.randomUUID());
            
            // Set Google ID if available
            if (request.getGoogleId() != null) {
                newUser.setGoogleId(request.getGoogleId());
            }
            
            log.info(LOG_NEW_USER_CREATED, request.getUserEmail());
            return userRepository.save(newUser);
        }
        
        return null;
    }
    
    /**
     * Create a UserImmigrationProfile from the request data
     */
    private UserImmigrationProfile createProfileFromRequest(ProfileSubmissionRequest request, User user) {
        log.info(LOG_PROFILE_START);
        
        UserImmigrationProfile profile = new UserImmigrationProfile();
        
        // Explicitly set UUID if not already set (to make service more robust)
        if (profile.getApplicationId() == null) {
            profile.setApplicationId(UUID.randomUUID());
            log.info(LOG_SETTING_UUID, profile.getApplicationId());
        }
        
        // Set the user on the profile
        profile.setUser(user);
        log.info(LOG_SETTING_USER, user.getId());
        
        // Set timestamps
        Instant now = Instant.now();
        profile.setCreatedAt(now);
        profile.setLastModifiedAt(now);
        
        // Set basic profile information
        setBasicProfileInfo(profile, request);
        
        // Set education information
        setEducationInfo(profile, request);
        
        // Set language information
        setLanguageInfo(profile, request);
        
        // Set work experience information
        setWorkExperienceInfo(profile, request);
        
        // Set partner information
        setPartnerInfo(profile, request);
        
        // Set job offer information
        setJobOfferInfo(profile, request);
        
        // Set provincial and additional information
        setProvincialAndAdditionalInfo(profile, request);
        
        // Store the complete JSON payload
        setJsonPayload(profile, request);
        
        // Set TEER categories from NOC codes
        setTeerCategoriesFromNocCodes(profile, request);
        
        log.info(LOG_PROFILE_END);
        return profile;
    }
    
    /**
     * Set basic profile information from the request
     */
    private void setBasicProfileInfo(UserImmigrationProfile profile, ProfileSubmissionRequest request) {
        profile.setUserEmail(request.getUserEmail());
        profile.setApplicantName(request.getApplicantName());
        profile.setApplicantAge(request.getApplicantAge());
        profile.setApplicantCitizenship(request.getApplicantCitizenship());
        profile.setApplicantResidence(request.getApplicantResidence());
        profile.setApplicantMaritalStatus(request.getApplicantMaritalStatus());
        
        log.info(LOG_BASIC_INFO_SET, request.getApplicantName());
    }
    
    /**
     * Set education information from the request
     */
    private void setEducationInfo(UserImmigrationProfile profile, ProfileSubmissionRequest request) {
        profile.setApplicantEducationLevel(request.getApplicantEducationLevel());
        profile.setEducationCompletedInCanada(request.getEducationCompletedInCanada());
        profile.setCanadianEducationLevel(request.getCanadianEducationLevel());
        profile.setHasEducationalCredentialAssessment(request.getHasEducationalCredentialAssessment());
        profile.setTradesCertification(request.getTradesCertification());
        
        // Clean up Canadian education fields if education not completed in Canada
        if (Boolean.FALSE.equals(request.getEducationCompletedInCanada())) {
            profile.setCanadianEducationLevel(null);
        }
        
        log.info(LOG_EDUCATION_INFO_SET, request.getApplicantEducationLevel());
    }
    
    /**
     * Set language information from the request
     */
    private void setLanguageInfo(UserImmigrationProfile profile, ProfileSubmissionRequest request) {
        // Primary language
        profile.setPrimaryLanguageTestType(request.getPrimaryLanguageTestType());
        profile.setPrimaryTestSpeakingScore(request.getPrimaryTestSpeakingScore());
        profile.setPrimaryTestListeningScore(request.getPrimaryTestListeningScore());
        profile.setPrimaryTestReadingScore(request.getPrimaryTestReadingScore());
        profile.setPrimaryTestWritingScore(request.getPrimaryTestWritingScore());
        
        // Secondary language
        profile.setTookSecondaryLanguageTest(request.getTookSecondaryLanguageTest());
        
        // Clean up secondary language fields when tookSecondaryLanguageTest is false
        if (Boolean.FALSE.equals(request.getTookSecondaryLanguageTest())) {
            // If no secondary language test, ensure all secondary language fields are null
            profile.setSecondaryTestType(null);
            profile.setSecondaryTestSpeakingScore(null);
            profile.setSecondaryTestListeningScore(null);
            profile.setSecondaryTestReadingScore(null);
            profile.setSecondaryTestWritingScore(null);
            log.info("Cleared secondary language fields because tookSecondaryLanguageTest is false");
        } else {
            // Only set secondary language fields if a test was taken
            profile.setSecondaryTestType(request.getSecondaryTestType());
            profile.setSecondaryTestSpeakingScore(request.getSecondaryTestSpeakingScore());
            profile.setSecondaryTestListeningScore(request.getSecondaryTestListeningScore());
            profile.setSecondaryTestReadingScore(request.getSecondaryTestReadingScore());
            profile.setSecondaryTestWritingScore(request.getSecondaryTestWritingScore());
        }
        
        log.info(LOG_LANGUAGE_INFO_SET, request.getPrimaryLanguageTestType());
    }
    
    /**
     * Set work experience information from the request
     */
    private void setWorkExperienceInfo(UserImmigrationProfile profile, ProfileSubmissionRequest request) {
        profile.setCanadianWorkExperienceYears(request.getCanadianWorkExperienceYears());
        profile.setNocCodeCanadian(request.getNocCodeCanadian());
        profile.setForeignWorkExperienceYears(request.getForeignWorkExperienceYears());
        profile.setNocCodeForeign(request.getNocCodeForeign());
        profile.setWorkingInCanada(request.getWorkingInCanada());
        
        // Clean up work experience fields if needed
        if (request.getCanadianWorkExperienceYears() != null && request.getCanadianWorkExperienceYears() <= 0) {
            profile.setNocCodeCanadian(null);
        }
        
        if (request.getForeignWorkExperienceYears() != null && request.getForeignWorkExperienceYears() <= 0) {
            profile.setNocCodeForeign(null);
        }
        
        log.info("Set work experience information with Canadian years: {}, Foreign years: {}", 
                request.getCanadianWorkExperienceYears(), request.getForeignWorkExperienceYears());
    }
    
    /**
     * Set partner information from the request
     */
    private void setPartnerInfo(UserImmigrationProfile profile, ProfileSubmissionRequest request) {
        // Clean up partner fields when marital status is Single
        if ("Single".equalsIgnoreCase(request.getApplicantMaritalStatus())) {
            // If marital status is Single, ensure all partner fields are null
            profile.setPartnerEducationLevel(null);
            profile.setPartnerLanguageTestType(null);
            profile.setPartnerTestSpeakingScore(null);
            profile.setPartnerTestListeningScore(null);
            profile.setPartnerTestReadingScore(null);
            profile.setPartnerTestWritingScore(null);
            profile.setPartnerCanadianWorkExperienceYears(null);
            log.info("Cleared partner fields because marital status is Single");
        } else {
            // Only set partner fields if not Single
            profile.setPartnerEducationLevel(request.getPartnerEducationLevel());
            profile.setPartnerLanguageTestType(request.getPartnerLanguageTestType());
            profile.setPartnerTestSpeakingScore(request.getPartnerTestSpeakingScore());
            profile.setPartnerTestListeningScore(request.getPartnerTestListeningScore());
            profile.setPartnerTestReadingScore(request.getPartnerTestReadingScore());
            profile.setPartnerTestWritingScore(request.getPartnerTestWritingScore());
            profile.setPartnerCanadianWorkExperienceYears(request.getPartnerCanadianWorkExperienceYears());
            
            log.info("Set partner information with education level: {}", request.getPartnerEducationLevel());
        }
    }
    
    /**
     * Set job offer information from the request
     */
    private void setJobOfferInfo(UserImmigrationProfile profile, ProfileSubmissionRequest request) {
        Boolean hasJobOffer = request.getHasJobOffer();
        Integer jobOfferNocCode = request.getJobOfferNocCode();
        
        log.info("Setting job offer data: hasJobOffer={}, jobOfferNocCode={}", 
                hasJobOffer, jobOfferNocCode);
        
        profile.setHasJobOffer(hasJobOffer);
        
        // Clean up job offer fields when hasJobOffer is false
        if (Boolean.FALSE.equals(hasJobOffer)) {
            // If there's no job offer, ensure all job offer fields are null
            profile.setJobOfferNocCode(null);
            profile.setIsJobOfferLmiaApproved(null);
            profile.setJobOfferWageCad(null);
            profile.setJobofferOccupationTeerCategory(null);
            log.info("Cleared job offer fields because hasJobOffer is false");
        } else {
            // Only set job offer fields if there is a job offer
            profile.setJobOfferNocCode(jobOfferNocCode);
            profile.setIsJobOfferLmiaApproved(request.getIsJobOfferLmiaApproved());
            profile.setJobOfferWageCad(request.getJobOfferWageCAD());
            
            // Safety check for NOC code
            if (Boolean.TRUE.equals(hasJobOffer) && profile.getJobOfferNocCode() == null && jobOfferNocCode != null) {
                profile.setJobOfferNocCode(jobOfferNocCode);
                log.info("Re-applied job offer NOC code as a safety measure: {}", jobOfferNocCode);
            }
            
            // Try to extract from JSON if still null
            if (Boolean.TRUE.equals(hasJobOffer) && profile.getJobOfferNocCode() == null) {
                attemptToExtractNocCodeFromJson(profile, request);
            }
        }
        
        log.info("Job offer information set with NOC code: {}", profile.getJobOfferNocCode());
    }
    
    /**
     * Set provincial and additional information from the request
     */
    private void setProvincialAndAdditionalInfo(UserImmigrationProfile profile, ProfileSubmissionRequest request) {
        // Provincial information
        profile.setHasProvincialNomination(request.getHasProvincialNomination());
        profile.setProvinceOfInterest(request.getProvinceOfInterest());
        profile.setHasCanadianRelatives(request.getHasCanadianRelatives());
        profile.setRelationshipWithCanadianRelative(request.getRelationshipWithCanadianRelative());
        profile.setReceivedInvitationToApply(request.getReceivedInvitationToApply());
        
        // Clean up fields if needed
        if (Boolean.FALSE.equals(request.getHasCanadianRelatives())) {
            profile.setRelationshipWithCanadianRelative(null);
        }
        
        
        // Additional information
        profile.setSettlementFundsCad(request.getSettlementFundsCAD());
        profile.setPreferredCity(request.getPreferredCity());
        profile.setPreferredDestinationProvince(request.getPreferredDestinationProvince());
        
        log.info("Set provincial and additional information");
    }
    
    /**
     * Set JSON payload from the request
     */
    private void setJsonPayload(UserImmigrationProfile profile, ProfileSubmissionRequest request) {
        try {
            String jsonPayload = request.getJsonPayload();
            // Validate that it's proper JSON by parsing it
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.readTree(jsonPayload); // This will throw an exception if the JSON is invalid
            // Set the validated JSON payload
            profile.setJsonPayload(jsonPayload);
            log.info("JSON payload set successfully: {}", jsonPayload.substring(0, Math.min(100, jsonPayload.length())) + "...");
        } catch (JsonProcessingException e) {
            log.error("Error processing JSON payload: {}", e.getMessage());
            // If there's an error, set an empty JSON object
            profile.setJsonPayload("{}");
        }
    }
    
    /**
     * Attempt to extract the NOC code from JSON payload if it's missing
     */
    private void attemptToExtractNocCodeFromJson(UserImmigrationProfile profile, ProfileSubmissionRequest request) {
        try {
            String jsonPayload = request.getJsonPayload();
            if (jsonPayload != null && !jsonPayload.isEmpty()) {
                log.info("Attempting to extract job offer NOC code from JSON payload");
                // Try to parse directly from the jsonPayload string
                if (jsonPayload.contains("\"jobOfferNocCode\":")) {
                    String extract = jsonPayload.split("\"jobOfferNocCode\":")[1].split(",")[0].trim();
                    extract = extract.replace("\"", "").trim();
                    try {
                        Integer extractedCode = Integer.valueOf(extract);
                        log.info("Found NOC code in JSON payload: {}", extractedCode);
                        profile.setJobOfferNocCode(extractedCode);
                    } catch (NumberFormatException e) {
                        log.error("Failed to parse extracted NOC code: {}", extract);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in direct JSON extraction for NOC code", e);
        }
    }
    
    /**
     * Lookup and set TEER categories for all NOC codes in the profile
     * 
     * @param profile The profile to update
     * @param request The original submission request
     */
    private void setTeerCategoriesFromNocCodes(UserImmigrationProfile profile, ProfileSubmissionRequest request) {
        log.info("=================== START OF setTeerCategoriesFromNocCodes ===================");
        
        // Check if repository is available - if not, log and return early to prevent NullPointerException
        if (jobsNocRepository == null) {
            log.warn("JobsNocRepository is not available, skipping TEER category lookup");
            return;
        }
        
        // CRITICAL: Save existing NOC code before any processing
        Integer existingJobOfferNocCode = profile.getJobOfferNocCode();
        log.info("CRITICAL - Job offer NOC code at start of TEER category setting: {}", existingJobOfferNocCode);
        
        // Keep a reference to the original object identity for debugging
        int profileHashCode = System.identityHashCode(profile);
        log.info("DEBUG - Object identity of profile at start: {}", profileHashCode);
        
        log.info("Setting TEER categories for profile {} with applicationId {}", 
                 profile.getApplicantName(), profile.getApplicationId());
        
        // Log the current state of NOC codes and TEER categories
        log.info("Current NOC codes: Canadian={}, Foreign={}, JobOffer={}", 
                 profile.getNocCodeCanadian(), profile.getNocCodeForeign(), profile.getJobOfferNocCode());
        log.info("Current TEER categories: Canadian={}, Foreign={}, JobOffer={}", 
                 profile.getCanadianOccupationTeerCategory(), 
                 profile.getForeignOccupationTeerCategory(), 
                 profile.getJobofferOccupationTeerCategory());
        
        try {
            // Canadian work experience NOC
            if (profile.getNocCodeCanadian() != null) {
                try {
                    Integer canadianNocCode = profile.getNocCodeCanadian();
                    log.info("Looking up TEER category for Canadian NOC code: {}", canadianNocCode);
                    
                    // First try with Integer directly
                    Optional<JobsNoc> jobNoc = jobsNocRepository.findByNocCode(canadianNocCode);
                    
                    // If not found, try with String conversion as fallback
                    if (jobNoc.isEmpty()) {
                        log.info("Retrying with string conversion for Canadian NOC code: {}", canadianNocCode);
                        jobNoc = jobsNocRepository.findByNocCodeString(canadianNocCode.toString());
                    }
                    
                    if (jobNoc.isPresent()) {
                        profile.setCanadianOccupationTeerCategory(jobNoc.get().getTeerCategory());
                        log.info("Set Canadian occupation TEER category to {} for NOC code {}", 
                                jobNoc.get().getTeerCategory(), canadianNocCode);
                        log.info("DEBUG - After processing Canadian NOC code, JobOffer NOC code={}", profile.getJobOfferNocCode());
                        log.info("DEBUG - Profile identity check: {}", System.identityHashCode(profile) == profileHashCode ? "SAME OBJECT" : "DIFFERENT OBJECT");
                    } else {
                        log.warn("No TEER category found in database for Canadian NOC code: {}", canadianNocCode);
                    }
                } catch (Exception e) {
                    log.error("Error looking up TEER category for Canadian NOC code: {}", profile.getNocCodeCanadian(), e);
                    // Continue processing, don't let this error fail the whole transaction
                }
            } else {
                log.info("No Canadian NOC code provided, skipping TEER category lookup");
            }
            
            // Foreign work experience NOC - Now also Integer
            if (profile.getNocCodeForeign() != null) {
                try {
                    Integer foreignNocCode = profile.getNocCodeForeign();
                    log.info("Looking up TEER category for Foreign NOC code: {}", foreignNocCode);
                    
                    // First try with Integer directly
                    Optional<JobsNoc> jobNoc = jobsNocRepository.findByNocCode(foreignNocCode);
                    
                    // If not found, try with String conversion as fallback
                    if (jobNoc.isEmpty()) {
                        log.info("Retrying with string conversion for Foreign NOC code: {}", foreignNocCode);
                        jobNoc = jobsNocRepository.findByNocCodeString(foreignNocCode.toString());
                    }
                    
                    if (jobNoc.isPresent()) {
                        profile.setForeignOccupationTeerCategory(jobNoc.get().getTeerCategory());
                        log.info("Set foreign occupation TEER category to {} for NOC code {}", 
                                jobNoc.get().getTeerCategory(), foreignNocCode);
                        log.info("DEBUG - After processing Foreign NOC code, JobOffer NOC code={}", profile.getJobOfferNocCode());
                        log.info("DEBUG - Profile identity check: {}", System.identityHashCode(profile) == profileHashCode ? "SAME OBJECT" : "DIFFERENT OBJECT");
                    } else {
                        log.warn("No TEER category found in database for foreign NOC code: {}", foreignNocCode);
                    }
                } catch (Exception e) {
                    log.error("Error looking up TEER category for foreign NOC code: {}", profile.getNocCodeForeign(), e);
                    // Continue processing, don't let this error fail the whole transaction
                }
            } else {
                log.info("No foreign NOC code provided, skipping TEER category lookup");
            }
            
            // Job offer NOC - Now also Integer
            // CRITICAL: Use the saved value if the profile one is null
            if (profile.getJobOfferNocCode() == null && existingJobOfferNocCode != null) {
                log.info("CRITICAL - Restoring job offer NOC code that was lost: {}", existingJobOfferNocCode);
                profile.setJobOfferNocCode(existingJobOfferNocCode);
            }
            
            // Double-check the profile object identity again
            log.info("DEBUG - Profile identity before job offer processing: {}", System.identityHashCode(profile) == profileHashCode ? "SAME OBJECT" : "DIFFERENT OBJECT");
            
            // Log the job offer fields again to verify their state
            log.info("DEBUG - Job offer fields before processing: hasJobOffer={}, jobOfferNocCode={}", profile.isHasJobOffer(), profile.getJobOfferNocCode());
            
            if (profile.getJobOfferNocCode() != null) {
                try {
                    Integer jobOfferNocCode = profile.getJobOfferNocCode();
                    log.info("Looking up TEER category for Job Offer NOC code: {}", jobOfferNocCode);
                    
                    // First try with Integer directly
                    Optional<JobsNoc> jobNoc = jobsNocRepository.findByNocCode(jobOfferNocCode);
                    
                    // If not found, try with String conversion as fallback
                    if (jobNoc.isEmpty()) {
                        log.info("Retrying with string conversion for Job Offer NOC code: {}", jobOfferNocCode);
                        jobNoc = jobsNocRepository.findByNocCodeString(jobOfferNocCode.toString());
                    }
                    
                    if (jobNoc.isPresent()) {
                        profile.setJobofferOccupationTeerCategory(jobNoc.get().getTeerCategory());
                        log.info("Set job offer occupation TEER category to {} for NOC code {}", 
                                jobNoc.get().getTeerCategory(), jobOfferNocCode);
                        log.info("DEBUG - After setting TEER category, JobOffer NOC code={}", profile.getJobOfferNocCode());
                    } else {
                        log.warn("No TEER category found in database for job offer NOC code: {}", jobOfferNocCode);
                    }
                } catch (Exception e) {
                    log.error("Error looking up TEER category for job offer NOC code: {}", profile.getJobOfferNocCode(), e);
                    // Continue processing, don't let this error fail the whole transaction
                }
                
                log.info("DEBUG - After job offer processing, JobOffer NOC code={}", profile.getJobOfferNocCode());
            } else {
                // Double-check request object directly as a fallback
                Integer requestJobOfferNocCode = request.getJobOfferNocCode();
                log.info("DEBUG - Job offer NOC code from request (fallback): {}", requestJobOfferNocCode);
                
                if (request.getHasJobOffer() != null && request.getHasJobOffer() && requestJobOfferNocCode != null) {
                    log.info("Found job offer NOC code in request object: {}", requestJobOfferNocCode);
                    
                    // Set it on the profile
                    profile.setJobOfferNocCode(requestJobOfferNocCode);
                    log.info("DEBUG - After setting from request, JobOffer NOC code={}", profile.getJobOfferNocCode());
                    
                    // Look up TEER category
                    try {
                        Optional<JobsNoc> jobNoc = jobsNocRepository.findByNocCode(requestJobOfferNocCode);
                        if (jobNoc.isEmpty()) {
                            jobNoc = jobsNocRepository.findByNocCodeString(requestJobOfferNocCode.toString());
                        }
                        
                        if (jobNoc.isPresent()) {
                            profile.setJobofferOccupationTeerCategory(jobNoc.get().getTeerCategory());
                            log.info("Set job offer occupation TEER category to {} for NOC code {} (from request)", 
                                    jobNoc.get().getTeerCategory(), requestJobOfferNocCode);
                            log.info("DEBUG - After setting TEER category from request, JobOffer NOC code={}", profile.getJobOfferNocCode());
                        } else {
                            log.warn("No TEER category found for job offer NOC code from request: {}", requestJobOfferNocCode);
                        }
                    } catch (Exception e) {
                        log.error("Error looking up TEER category for job offer NOC code from request: {}", requestJobOfferNocCode, e);
                }
            } else {
                log.info("No job offer NOC code provided, skipping TEER category lookup");
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error while setting TEER categories from NOC codes", e);
            // Log but don't rethrow to prevent transaction rollback
        }
        
        // Final verification
        log.info("DEBUG - Final profile identity check: {}", System.identityHashCode(profile) == profileHashCode ? "SAME OBJECT" : "DIFFERENT OBJECT");
        
        // CRITICAL - One more time, restore the job offer NOC code if it was lost
        if (profile.getJobOfferNocCode() == null && existingJobOfferNocCode != null) {
            log.info("CRITICAL - Final restoration of job offer NOC code that was lost: {}", existingJobOfferNocCode);
            profile.setJobOfferNocCode(existingJobOfferNocCode);
        }
        
        // Log final state of TEER categories
        log.info("Final TEER categories: Canadian={}, Foreign={}, JobOffer={}", 
                 profile.getCanadianOccupationTeerCategory(), 
                 profile.getForeignOccupationTeerCategory(), 
                 profile.getJobofferOccupationTeerCategory());
        
        log.info("Final NOC codes: Canadian={}, Foreign={}, JobOffer={}", 
                profile.getNocCodeCanadian(), profile.getNocCodeForeign(), profile.getJobOfferNocCode());
                
        log.info("=================== END OF setTeerCategoriesFromNocCodes ===================");
    }

    /**
     * Validate a profile submission request
     * 
     * @param request The profile submission request to validate
     * @return An Optional containing an error message, or empty if validation passes
     */
    public Optional<String> validateProfileSubmission(ProfileSubmissionRequest request) {
        log.info("Validating profile submission request: {}", request);
        
        List<String> validationErrors = new ArrayList<>();
        
        // Validate applicant name
        if (request.getApplicantName() == null || request.getApplicantName().trim().isEmpty()) {
            validationErrors.add(String.format(VALIDATION_REQUIRED_FIELD, "Applicant name"));
        }
        
        // Validate applicant age - must be positive and reasonable
        if (request.getApplicantAge() == null) {
            validationErrors.add(String.format(VALIDATION_REQUIRED_FIELD, "Applicant age"));
        } else if (request.getApplicantAge() <= 0) {
            validationErrors.add(String.format(VALIDATION_POSITIVE, "Applicant age"));
        } else if (request.getApplicantAge() > MAX_REASONABLE_AGE) {
            validationErrors.add("Applicant age must be reasonable (under " + MAX_REASONABLE_AGE + ")");
        }
        
        // Validate language scores
        validateLanguageScores(request, validationErrors);
        
        // Validate educational credential assessment
        validateEducationalCredentialAssessment(request, validationErrors);
        
        // Validate job offer consistency
        validateJobOffer(request, validationErrors);
        
        // Validate settlement funds (must be non-negative)
        if (request.getSettlementFundsCAD() != null && request.getSettlementFundsCAD() < 0) {
            validationErrors.add("Settlement funds cannot be negative");
        }
        
        // Validate foreign work experience
        validateForeignWorkExperience(request, validationErrors);
        
        // Validate secondary language test
        validateSecondaryLanguageTest(request, validationErrors);
        
        // Validate partner data consistency with marital status
        validatePartnerData(request, validationErrors);
        
        // Validate education completed in Canada
        validateCanadianEducation(request, validationErrors);
        
        // Validate provincial nomination
        validateProvincialNomination(request, validationErrors);
        
        // Validate Canadian relatives
        validateCanadianRelatives(request, validationErrors);
        
        // Validate working in Canada
        validateWorkingInCanada(request, validationErrors);
        
        // Return the first validation error if any exist
        if (!validationErrors.isEmpty()) {
            return Optional.of(validationErrors.get(0));
        }
        
        return Optional.empty();
    }
    
    /**
     * Validate that language scores are within acceptable ranges
     */
    private void validateLanguageScores(ProfileSubmissionRequest request, List<String> validationErrors) {
        // Check if at least one primary language score is provided but not all
        boolean hasSomePrimaryScores = request.getPrimaryTestSpeakingScore() != null || 
                                      request.getPrimaryTestListeningScore() != null ||
                                      request.getPrimaryTestReadingScore() != null || 
                                      request.getPrimaryTestWritingScore() != null;
        
        boolean hasAllPrimaryScores = request.getPrimaryTestSpeakingScore() != null && 
                                     request.getPrimaryTestListeningScore() != null &&
                                     request.getPrimaryTestReadingScore() != null && 
                                     request.getPrimaryTestWritingScore() != null;
        
        // If some scores are missing but not all, flag it as an error
        if (hasSomePrimaryScores && !hasAllPrimaryScores) {
            validationErrors.add("All primary language test scores (speaking, listening, reading, writing) must be provided");
            log.warn("Validation warning: Incomplete primary language test scores provided");
        }
        
        // Primary language test scores - should be between MIN_LANGUAGE_SCORE and MAX_LANGUAGE_SCORE (CLB)
        if (request.getPrimaryTestSpeakingScore() != null && 
            (request.getPrimaryTestSpeakingScore() < MIN_LANGUAGE_SCORE || request.getPrimaryTestSpeakingScore() > MAX_LANGUAGE_SCORE)) {
            validationErrors.add(String.format(VALIDATION_SCORE_RANGE, "Primary speaking", MIN_LANGUAGE_SCORE, MAX_LANGUAGE_SCORE));
        }
        
        if (request.getPrimaryTestListeningScore() != null && 
            (request.getPrimaryTestListeningScore() < MIN_LANGUAGE_SCORE || request.getPrimaryTestListeningScore() > MAX_LANGUAGE_SCORE)) {
            validationErrors.add(String.format(VALIDATION_SCORE_RANGE, "Primary listening", MIN_LANGUAGE_SCORE, MAX_LANGUAGE_SCORE));
        }
        
        if (request.getPrimaryTestReadingScore() != null && 
            (request.getPrimaryTestReadingScore() < MIN_LANGUAGE_SCORE || request.getPrimaryTestReadingScore() > MAX_LANGUAGE_SCORE)) {
            validationErrors.add(String.format(VALIDATION_SCORE_RANGE, "Primary reading", MIN_LANGUAGE_SCORE, MAX_LANGUAGE_SCORE));
        }
        
        if (request.getPrimaryTestWritingScore() != null && 
            (request.getPrimaryTestWritingScore() < MIN_LANGUAGE_SCORE || request.getPrimaryTestWritingScore() > MAX_LANGUAGE_SCORE)) {
            validationErrors.add(String.format(VALIDATION_SCORE_RANGE, "Primary writing", MIN_LANGUAGE_SCORE, MAX_LANGUAGE_SCORE));
        }
    }
    
    /**
     * Validate educational credential assessment information
     */
    private void validateEducationalCredentialAssessment(ProfileSubmissionRequest request, List<String> validationErrors) {
        // Check educational credential assessment logic
        Boolean hasEca = request.getHasEducationalCredentialAssessment();
        String applicantEducationLevel = request.getApplicantEducationLevel();
        
        // If foreign education and no ECA, warn the user (this is a soft validation)
        if (Boolean.FALSE.equals(request.getEducationCompletedInCanada()) && 
            (hasEca == null || !hasEca) && 
            applicantEducationLevel != null && 
            !applicantEducationLevel.equalsIgnoreCase("high-school")) {
            
            validationErrors.add("Foreign education without ECA: You have indicated foreign education but no Educational Credential Assessment (ECA). An ECA is typically required for foreign education credentials.");
        }
    }
    
    /**
     * Validate that job offer information is consistent.
     * If hasJobOffer is true, the NOC code must be provided.
     */
    private void validateJobOffer(ProfileSubmissionRequest request, List<String> validationErrors) {
        Boolean hasJobOffer = request.getHasJobOffer();
        Integer jobOfferNocCode = request.getJobOfferNocCode();
        Boolean isLmiaApproved = request.getIsJobOfferLmiaApproved();
        Integer wage = request.getJobOfferWageCAD();
        
        if (Boolean.TRUE.equals(hasJobOffer)) {
            // If has job offer is true, NOC code must be provided
            if (jobOfferNocCode == null) {
                validationErrors.add("Job offer selected but no NOC code provided");
                log.error("Validation failed: Job offer selected but no NOC code provided");
            }
            
            // Wage should be positive if provided
            if (wage != null && wage <= 0) {
                validationErrors.add("Job offer wage must be positive");
            }
        } else if (Boolean.FALSE.equals(hasJobOffer)) {
            // If no job offer, ensure job offer fields are null/empty
            if (jobOfferNocCode != null || isLmiaApproved != null || wage != null) {
                log.warn("Inconsistent data: No job offer selected but job data provided - NOC:{}, LMIA:{}, Wage:{}", 
                        jobOfferNocCode, isLmiaApproved, wage);
                // This will be cleaned during profile creation
                // Do not add to validation errors since this will be cleaned up
            }
        }
    }
    
    /**
     * Validate that foreign work experience information is consistent.
     * If foreignWorkExperienceYears > 0, the NOC code must be provided.
     */
    private void validateForeignWorkExperience(ProfileSubmissionRequest request, List<String> validationErrors) {
        Integer foreignYears = request.getForeignWorkExperienceYears();
        Integer foreignNoc = request.getNocCodeForeign();
        
        if (foreignYears != null && foreignYears > 0) {
            // If foreign work experience is indicated, NOC code must be provided
            if (foreignNoc == null) {
                validationErrors.add("Foreign work experience years > 0 but no NOC code provided");
                log.error("Validation failed: Foreign work experience years > 0 but no NOC code provided");
            }
        } else {
            // If no foreign work experience, ensure NOC code is null
            if (foreignNoc != null) {
                log.warn("Inconsistent data: No foreign work experience selected but NOC code provided: {}. This will be cleared.", foreignNoc);
                // This will be cleaned during profile creation
            }
        }
    }
    
    /**
     * Validate that secondary language test information is consistent.
     * If tookSecondaryLanguageTest is true, then all test scores must be provided and valid.
     * Additionally, secondary test type cannot be the same as primary test type.
     */
    private void validateSecondaryLanguageTest(ProfileSubmissionRequest request, List<String> validationErrors) {
        Boolean tookSecondaryTest = request.getTookSecondaryLanguageTest();
        String secondaryTestType = request.getSecondaryTestType();
        Integer speakingScore = request.getSecondaryTestSpeakingScore();
        Integer listeningScore = request.getSecondaryTestListeningScore();
        Integer readingScore = request.getSecondaryTestReadingScore();
        Integer writingScore = request.getSecondaryTestWritingScore();
        
        if (Boolean.TRUE.equals(tookSecondaryTest)) {
            // If secondary test taken, type must be provided
            if (secondaryTestType == null || secondaryTestType.trim().isEmpty()) {
                validationErrors.add("Secondary language test type is required when secondary test is indicated");
            }
            
            // Check that secondary language test type is not the same as primary
            if (secondaryTestType != null && !secondaryTestType.trim().isEmpty() && 
                secondaryTestType.equals(request.getPrimaryLanguageTestType())) {
                validationErrors.add("Secondary language test type cannot be the same as primary test type");
            }
            
            // Validate that all scores are provided and within range
            if (speakingScore == null) {
                validationErrors.add("Secondary language speaking score is required when secondary test is indicated");
            } else if (speakingScore < MIN_LANGUAGE_SCORE || speakingScore > MAX_LANGUAGE_SCORE) {
                validationErrors.add(String.format(VALIDATION_SCORE_RANGE, "Secondary speaking", MIN_LANGUAGE_SCORE, MAX_LANGUAGE_SCORE));
            }
            
            if (listeningScore == null) {
                validationErrors.add("Secondary language listening score is required when secondary test is indicated");
            } else if (listeningScore < MIN_LANGUAGE_SCORE || listeningScore > MAX_LANGUAGE_SCORE) {
                validationErrors.add(String.format(VALIDATION_SCORE_RANGE, "Secondary listening", MIN_LANGUAGE_SCORE, MAX_LANGUAGE_SCORE));
            }
            
            if (readingScore == null) {
                validationErrors.add("Secondary language reading score is required when secondary test is indicated");
            } else if (readingScore < MIN_LANGUAGE_SCORE || readingScore > MAX_LANGUAGE_SCORE) {
                validationErrors.add(String.format(VALIDATION_SCORE_RANGE, "Secondary reading", MIN_LANGUAGE_SCORE, MAX_LANGUAGE_SCORE));
            }
            
            if (writingScore == null) {
                validationErrors.add("Secondary language writing score is required when secondary test is indicated");
            } else if (writingScore < MIN_LANGUAGE_SCORE || writingScore > MAX_LANGUAGE_SCORE) {
                validationErrors.add(String.format(VALIDATION_SCORE_RANGE, "Secondary writing", MIN_LANGUAGE_SCORE, MAX_LANGUAGE_SCORE));
            }
        } else if (Boolean.FALSE.equals(tookSecondaryTest)) {
            // If no secondary test, ensure all secondary test fields are null/empty
            if (secondaryTestType != null && !secondaryTestType.trim().isEmpty() || 
                speakingScore != null || listeningScore != null || 
                readingScore != null || writingScore != null) {
                log.warn("Inconsistent data: No secondary test indicated but test data provided. This will be cleaned during profile creation.");
                // This will be cleaned during profile creation
                // Do not add to validation errors since this will be cleaned up
            }
        }
    }
    
    /**
     * Validate that partner data is consistent with marital status.
     * If marital status is "Single", partner fields should be null/empty.
     */
    private void validatePartnerData(ProfileSubmissionRequest request, List<String> validationErrors) {
        String maritalStatus = request.getApplicantMaritalStatus();
        String partnerEducation = request.getPartnerEducationLevel();
        String partnerTestType = request.getPartnerLanguageTestType();
        Integer partnerSpeaking = request.getPartnerTestSpeakingScore();
        Integer partnerListening = request.getPartnerTestListeningScore();
        Integer partnerReading = request.getPartnerTestReadingScore();
        Integer partnerWriting = request.getPartnerTestWritingScore();
        Integer partnerCanadianYears = request.getPartnerCanadianWorkExperienceYears();
        
        if (MARITAL_STATUS_SINGLE.equalsIgnoreCase(maritalStatus)) {
            // If single, ensure all partner fields are null/empty
            if (partnerEducation != null && !partnerEducation.trim().isEmpty() || 
                partnerTestType != null && !partnerTestType.trim().isEmpty() || 
                partnerSpeaking != null || partnerListening != null || 
                partnerReading != null || partnerWriting != null || 
                partnerCanadianYears != null) {
                validationErrors.add("Partner data provided for a single person");
                log.error("Validation failed: Single applicant but partner data provided");
            }
        } else if (maritalStatus != null && !MARITAL_STATUS_SINGLE.equalsIgnoreCase(maritalStatus)) {
            // For non-single applicants, partner fields are optional but should be validated if provided
            
            // If partner test type is provided, at least one score should be present
            if (partnerTestType != null && !partnerTestType.trim().isEmpty() &&
                partnerSpeaking == null && partnerListening == null && 
                partnerReading == null && partnerWriting == null) {
                validationErrors.add("At least one partner language test score must be provided when a partner test type is indicated");
            }
            
            // Validate ranges of provided scores
            if (partnerSpeaking != null && (partnerSpeaking < MIN_LANGUAGE_SCORE || partnerSpeaking > MAX_LANGUAGE_SCORE)) {
                validationErrors.add(String.format(VALIDATION_SCORE_RANGE, "Partner speaking", MIN_LANGUAGE_SCORE, MAX_LANGUAGE_SCORE));
            }
            
            if (partnerListening != null && (partnerListening < MIN_LANGUAGE_SCORE || partnerListening > MAX_LANGUAGE_SCORE)) {
                validationErrors.add(String.format(VALIDATION_SCORE_RANGE, "Partner listening", MIN_LANGUAGE_SCORE, MAX_LANGUAGE_SCORE));
            }
            
            if (partnerReading != null && (partnerReading < MIN_LANGUAGE_SCORE || partnerReading > MAX_LANGUAGE_SCORE)) {
                validationErrors.add(String.format(VALIDATION_SCORE_RANGE, "Partner reading", MIN_LANGUAGE_SCORE, MAX_LANGUAGE_SCORE));
            }
            
            if (partnerWriting != null && (partnerWriting < MIN_LANGUAGE_SCORE || partnerWriting > MAX_LANGUAGE_SCORE)) {
                validationErrors.add(String.format(VALIDATION_SCORE_RANGE, "Partner writing", MIN_LANGUAGE_SCORE, MAX_LANGUAGE_SCORE));
            }
            
            // Partner Canadian work experience years should be non-negative if provided
            if (partnerCanadianYears != null && partnerCanadianYears < 0) {
                validationErrors.add(String.format(VALIDATION_NON_NEGATIVE, "Partner Canadian work experience years"));
            }
        }
    }
    
    /**
     * Validate that Canadian education information is consistent.
     * If educationCompletedInCanada is true, canadianEducationLevel must be provided.
     */
    private void validateCanadianEducation(ProfileSubmissionRequest request, List<String> validationErrors) {
        Boolean educationInCanada = request.getEducationCompletedInCanada();
        String canadianLevel = request.getCanadianEducationLevel();
        
        if (Boolean.TRUE.equals(educationInCanada)) {
            // If education completed in Canada, level must be provided
            if (canadianLevel == null || canadianLevel.trim().isEmpty()) {
                validationErrors.add("Canadian education level is required when education completed in Canada is indicated");
                log.error("Validation failed: Canadian education selected but no level provided");
            }
        } else if (Boolean.FALSE.equals(educationInCanada)) {
            // If no education in Canada, ensure level is null/empty
            if (canadianLevel != null && !canadianLevel.trim().isEmpty()) {
                log.warn("Inconsistent data: No Canadian education selected but level provided: {}. This will be cleared.", canadianLevel);
                // This will be cleaned during profile creation
            }
        }
    }
    
    /**
     * Validate that provincial nomination information is consistent.
     * If hasProvincialNomination is true, provinceOfInterest must be provided.
     */
    private void validateProvincialNomination(ProfileSubmissionRequest request, List<String> validationErrors) {
        Boolean hasNomination = request.getHasProvincialNomination();
        String province = request.getProvinceOfInterest();
        
        if (Boolean.TRUE.equals(hasNomination)) {
            // If provincial nomination is indicated, province must be provided
            if (province == null || province.trim().isEmpty()) {
                validationErrors.add("Provincial nomination selected but no province provided");
                log.error("Validation failed: Provincial nomination selected but no province provided");
            }
        }
    }
    
    /**
     * Validate that working in Canada information is consistent.
     * If workingInCanada is true, appropriate work experience should be indicated.
     */
    private void validateWorkingInCanada(ProfileSubmissionRequest request, List<String> validationErrors) {
        Boolean workingInCanada = request.getWorkingInCanada();
        Integer canadianYears = request.getCanadianWorkExperienceYears();
        
        if (Boolean.TRUE.equals(workingInCanada)) {
            // If currently working in Canada, should have some Canadian work experience
            if (canadianYears == null || canadianYears <= 0) {
                validationErrors.add("Canadian work experience years should be positive when currently working in Canada");
                log.warn("Inconsistent data: Working in Canada but no Canadian work experience years provided");
            }
        }
        
        // Canadian work experience years should be non-negative if provided
        if (canadianYears != null && canadianYears < 0) {
            validationErrors.add("Canadian work experience years must be non-negative");
        }
    }

    /**
     * Validate that Canadian relatives information is consistent.
     * If hasCanadianRelatives is true, a relationship must be specified.
     */
    private void validateCanadianRelatives(ProfileSubmissionRequest request, List<String> validationErrors) {
        Boolean hasCanadianRelatives = request.getHasCanadianRelatives();
        String relationship = request.getRelationshipWithCanadianRelative();
        
        if (Boolean.TRUE.equals(hasCanadianRelatives)) {
            if (relationship == null || relationship.trim().isEmpty()) {
                validationErrors.add("Canadian relative selected but no relationship with Canadian relative specified");
                log.error("Validation failed: Canadian relative selected but no relationship specified");
            }
        } else if (Boolean.FALSE.equals(hasCanadianRelatives)) {
            // If no Canadian relatives, ensure relationship is null/empty
            if (relationship != null && !relationship.trim().isEmpty()) {
                log.warn("Inconsistent data: No Canadian relatives selected but relationship provided: {}. This will be cleared.", relationship);
                // This will be cleaned during profile creation
            }
        }
    }

    /**
     * Save form draft for a user, finding the user by email or Google ID.
     * 
     * @param userEmail The email of the user
     * @param googleId The Google ID of the user (from OAuth2)
     * @param formDataJson The form data as a JSON string
     * @return The saved draft
     * @throws IllegalArgumentException If no user is found by email or Google ID
     */
    @Transactional
    public ProfileDraft saveProfileDraftWithGoogleId(String userEmail, String googleId, String formDataJson) {
        log.debug("Saving profile draft for user - Email: {}, Google ID: {}", userEmail, googleId);
        
        // Try to find the user by email or Google ID
        User user = null;
        
        // First try by email
        if (userEmail != null && !userEmail.isEmpty()) {
            Optional<User> userByEmail = userRepository.findByEmail(userEmail);
            if (userByEmail.isPresent()) {
                user = userByEmail.get();
                log.info("Found user by email: {}", userEmail);
            }
        }
        
        // If not found by email, try by Google ID
        if (user == null && googleId != null && !googleId.isEmpty()) {
            Optional<User> userByGoogleId = userRepository.findByGoogleId(googleId);
            if (userByGoogleId.isPresent()) {
                user = userByGoogleId.get();
                log.info("Found user by Google ID: {}", googleId);
            }
        }
        
        // If still not found, throw an exception
        if (user == null) {
            String errorMsg = "User not found - Email: " + userEmail + ", Google ID: " + googleId;
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        
        // Create a new draft
        ProfileDraft draft = ProfileDraft.builder()
                .user(user)
                .userEmail(userEmail != null ? userEmail : user.getEmail()) // Use user.getEmail() as fallback
                .formDataJson(formDataJson)
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .build();
        
        // Save and return the draft
        return profileDraftRepository.save(draft);
    }
    
    /**
     * Get the latest profile form draft for a user, finding the user by email or Google ID.
     * 
     * @param userEmail The email of the user
     * @param googleId The Google ID of the user (from OAuth2)
     * @return Optional containing the form data JSON if found
     */
    @Transactional(readOnly = true)
    public Optional<String> getLatestProfileDraftWithGoogleId(String userEmail, String googleId) {
        log.debug("Retrieving latest profile draft for user - Email: {}, Google ID: {}", userEmail, googleId);
        
        // First try by email
        if (userEmail != null && !userEmail.isEmpty()) {
            Optional<ProfileDraft> latestDraftByEmail = profileDraftRepository.findFirstByUserEmailOrderByLastModifiedAtDesc(userEmail);
            if (latestDraftByEmail.isPresent()) {
                log.info("Found latest draft by email: {}", userEmail);
                return latestDraftByEmail.map(ProfileDraft::getFormDataJson);
            }
        }
        
        // If not found by email and we have a Google ID, try finding the user by Google ID
        if (googleId != null && !googleId.isEmpty()) {
            // Find the user by Google ID
            Optional<User> userByGoogleId = userRepository.findByGoogleId(googleId);
            if (userByGoogleId.isPresent()) {
                User user = userByGoogleId.get();
                log.info("Found user by Google ID: {}", googleId);
                
                // Get drafts by user
                Optional<ProfileDraft> latestDraftByUser = profileDraftRepository.findFirstByUserOrderByLastModifiedAtDesc(user);
                if (latestDraftByUser.isPresent()) {
                    log.info("Found latest draft by user with Google ID: {}", googleId);
                    return latestDraftByUser.map(ProfileDraft::getFormDataJson);
                }
            }
        }
        
        // If we reach here, no draft was found
        log.info("No draft found for user - Email: {}, Google ID: {}", userEmail, googleId);
        return Optional.empty();
    }
    
    /**
     * Save form draft for a user.
     * 
     * @param userEmail The email of the user
     * @param formDataJson The form data as a JSON string
     * @return The saved draft
     * @throws IllegalArgumentException If the user is not found
     */
    @Transactional
    public ProfileDraft saveProfileDraft(String userEmail, String formDataJson) {
        log.debug(LOG_SAVING_DRAFT, userEmail);
        
        // Forward to the more comprehensive method
        return saveProfileDraftWithGoogleId(userEmail, null, formDataJson);
    }
    
    /**
     * Get the latest profile form draft for a user.
     * 
     * @param userEmail The email of the user
     * @return Optional containing the form data JSON if found
     */
    @Transactional(readOnly = true)
    public Optional<String> getLatestProfileDraft(String userEmail) {
        log.debug(LOG_RETRIEVING_DRAFT, userEmail);
        
        // Forward to the more comprehensive method
        return getLatestProfileDraftWithGoogleId(userEmail, null);
    }
    
    /**
     * Delete all profile form drafts for a user.
     * 
     * @param userEmail The email of the user
     */
    @Transactional
    public void deleteAllProfileDrafts(String userEmail) {
        log.debug(LOG_DELETING_DRAFTS, userEmail);
        
        // Find all drafts for the user
        List<ProfileDraft> drafts = profileDraftRepository.findByUserEmail(userEmail);
        
        // Delete all drafts
        profileDraftRepository.deleteAll(drafts);
    }
}
