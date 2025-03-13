package co.immimate.profile.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.immimate.profile.dto.ProfileSubmissionRequest;
import co.immimate.profile.dto.ProfileSubmissionResponse;
import co.immimate.profile.model.UserImmigrationProfile;
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
    private final UserImmigrationProfileRepository profileRepository;
    private final UserRepository userRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * Constructor for ProfileService.
     * 
     * @param profileRepository Repository for accessing profile data
     * @param userRepository Repository for accessing user data
     */
    @Autowired
    public ProfileService(UserImmigrationProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
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
        log.info("Fetching most recent profile for user email: {}", email);
        return profileRepository.findFirstByUserEmailOrderByCreatedAtDesc(email);
    }

    /**
     * Submit a new immigration profile for a user
     */
    @Transactional
    public ProfileSubmissionResponse submitProfile(ProfileSubmissionRequest request) {
        log.info("Processing profile submission for email: {}", request.getUserEmail());
        
        try {
            // Validate the request
            Optional<String> validationError = validateProfileRequest(request);
            if (validationError.isPresent()) {
                log.warn("Validation failed for profile submission: {}", validationError.get());
                return ProfileSubmissionResponse.error(validationError.get());
            }
            
            // Find or create user
            User user = findUserByIdOrEmail(request);
            if (user == null) {
                // Create a temporary user if not found
                log.info("User not found, creating temporary user with email: {}", request.getUserEmail());
                user = new User();
                user.setEmail(request.getUserEmail());
                user.setRole("USER");
                user = userRepository.save(user);
                log.info("Created new user with ID: {}", user.getId());
            } else {
                log.info("Found existing user with ID: {}", user.getId());
            }
            
            // Create profile from request
            UserImmigrationProfile profile = createProfileFromRequest(request, user);
            
            // With the @Type annotation, Hibernate will handle the JSONB conversion automatically
            profile = profileRepository.save(profile);
            log.info("Saved profile with ID: {} for user: {}", profile.getApplicationId(), user.getId());
            
            // Return success response
            return ProfileSubmissionResponse.success(
                profile.getApplicationId(),
                "Profile submitted successfully"
            );
        } catch (Exception e) {
            log.error("Error saving profile: {}", e.getMessage(), e);
            return ProfileSubmissionResponse.error("Error saving profile: " + e.getMessage());
        }
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
        
        return null;
    }
    
    /**
     * Create a UserImmigrationProfile from the request data
     */
    private UserImmigrationProfile createProfileFromRequest(ProfileSubmissionRequest request, User user) {
        UserImmigrationProfile profile = new UserImmigrationProfile();
        
        // Set the user on the profile
        profile.setUser(user);
        log.info("Setting user on profile: userId={}", user.getId());
        
        // Set timestamps
        Instant now = Instant.now();
        
        // Set fields using setters
        profile.setUserEmail(request.getUserEmail());
        profile.setApplicantName(request.getApplicantName());
        profile.setApplicantAge(request.getApplicantAge());
        profile.setApplicantCitizenship(request.getApplicantCitizenship());
        profile.setApplicantResidence(request.getApplicantResidence());
        profile.setApplicantMaritalStatus(request.getApplicantMaritalStatus());
        
        // Education
        profile.setApplicantEducationLevel(request.getApplicantEducationLevel());
        profile.setEducationCompletedInCanada(request.getEducationCompletedInCanada());
        profile.setCanadianEducationLevel(request.getCanadianEducationLevel());
        profile.setHasEducationalCredentialAssessment(request.getHasEducationalCredentialAssessment());
        profile.setTradesCertification(request.getTradesCertification());
        
        // Language
        profile.setPrimaryLanguageTestType(request.getPrimaryLanguageTestType());
        profile.setPrimaryTestSpeakingScore(request.getPrimaryTestSpeakingScore());
        profile.setPrimaryTestListeningScore(request.getPrimaryTestListeningScore());
        profile.setPrimaryTestReadingScore(request.getPrimaryTestReadingScore());
        profile.setPrimaryTestWritingScore(request.getPrimaryTestWritingScore());
        
        profile.setTookSecondaryLanguageTest(request.getTookSecondaryLanguageTest());
        profile.setSecondaryTestType(request.getSecondaryTestType());
        profile.setSecondaryTestSpeakingScore(request.getSecondaryTestSpeakingScore());
        profile.setSecondaryTestListeningScore(request.getSecondaryTestListeningScore());
        profile.setSecondaryTestReadingScore(request.getSecondaryTestReadingScore());
        profile.setSecondaryTestWritingScore(request.getSecondaryTestWritingScore());
        
        // Work experience
        profile.setCanadianWorkExperienceYears(request.getCanadianWorkExperienceYears());
        profile.setNocCodeCanadian(request.getNocCodeCanadian());
        profile.setForeignWorkExperienceYears(request.getForeignWorkExperienceYears());
        profile.setNocCodeForeign(request.getNocCodeForeign());
        profile.setWorkingInCanada(request.getWorkingInCanada());
        
        // Partner information
        profile.setPartnerEducationLevel(request.getPartnerEducationLevel());
        profile.setPartnerLanguageTestType(request.getPartnerLanguageTestType());
        profile.setPartnerTestSpeakingScore(request.getPartnerTestSpeakingScore());
        profile.setPartnerTestListeningScore(request.getPartnerTestListeningScore());
        profile.setPartnerTestReadingScore(request.getPartnerTestReadingScore());
        profile.setPartnerTestWritingScore(request.getPartnerTestWritingScore());
        profile.setPartnerCanadianWorkExperienceYears(request.getPartnerCanadianWorkExperienceYears());
        
        // Provincial information
        profile.setHasProvincialNomination(request.getHasProvincialNomination());
        profile.setProvinceOfInterest(request.getProvinceOfInterest());
        profile.setHasCanadianRelatives(request.getHasCanadianRelatives());
        profile.setRelationshipWithCanadianRelative(request.getRelationshipWithCanadianRelative());
        profile.setReceivedInvitationToApply(request.getReceivedInvitationToApply());
        
        // Job information
        profile.setHasJobOffer(request.getHasJobOffer());
        profile.setIsJobOfferLmiaApproved(request.getIsJobOfferLmiaApproved());
        profile.setJobOfferWageCad(request.getJobOfferWageCAD());
        profile.setJobOfferNocCode(request.getJobOfferNocCode());
        
        // Additional information
        profile.setSettlementFundsCad(request.getSettlementFundsCAD());
        profile.setPreferredCity(request.getPreferredCity());
        profile.setPreferredDestinationProvince(request.getPreferredDestinationProvince());
        
        // Store the complete JSON payload
        // Make sure the JSON is valid before setting it
        try {
            String jsonPayload = request.getJsonPayload();
            // Validate that it's proper JSON by parsing it
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.readTree(jsonPayload); // This will throw an exception if the JSON is invalid
            // Set the validated JSON payload
            profile.setJsonPayload(jsonPayload);
            log.info("JSON payload set successfully: {}", jsonPayload.substring(0, Math.min(100, jsonPayload.length())) + "...");
        } catch (Exception e) {
            log.error("Error processing JSON payload: {}", e.getMessage());
            // If there's an error, set an empty JSON object
            profile.setJsonPayload("{}");
        }
        
        // Set creation and modification timestamps
        profile.setCreatedAt(now);
        profile.setLastModifiedAt(now);
        
        return profile;
    }

    /**
     * Validates a profile submission request against business rules
     * 
     * @param request The profile submission request to validate
     * @return Optional containing error message if validation fails, empty if valid
     */
    private Optional<String> validateProfileRequest(ProfileSubmissionRequest request) {
        // Check for missing required fields
        if (request.getApplicantName() == null || request.getApplicantName().trim().isEmpty()) {
            return Optional.of("Applicant name is required");
        }
        
        // Validate age
        if (request.getApplicantAge() == null || request.getApplicantAge() < 0) {
            return Optional.of("Invalid age: Age must be a positive number");
        }
        
        // Validate language scores (ranging from 1-12 for CLB)
        if (request.getPrimaryTestReadingScore() != null && (request.getPrimaryTestReadingScore() < 1 || request.getPrimaryTestReadingScore() > 12)) {
            return Optional.of("Invalid primary reading score: Must be between 1 and 12 (CLB)");
        }
        if (request.getPrimaryTestWritingScore() != null && (request.getPrimaryTestWritingScore() < 1 || request.getPrimaryTestWritingScore() > 12)) {
            return Optional.of("Invalid primary writing score: Must be between 1 and 12 (CLB)");
        }
        if (request.getPrimaryTestSpeakingScore() != null && (request.getPrimaryTestSpeakingScore() < 1 || request.getPrimaryTestSpeakingScore() > 12)) {
            return Optional.of("Invalid primary speaking score: Must be between 1 and 12 (CLB)");
        }
        if (request.getPrimaryTestListeningScore() != null && (request.getPrimaryTestListeningScore() < 1 || request.getPrimaryTestListeningScore() > 12)) {
            return Optional.of("Invalid primary listening score: Must be between 1 and 12 (CLB)");
        }
        
        // Validate job offer consistency
        if (Boolean.TRUE.equals(request.getHasJobOffer())) {
            if (request.getJobOfferNocCode() == null || request.getJobOfferNocCode().trim().isEmpty()) {
                return Optional.of("Job offer NOC code is required when job offer is indicated");
            }
            if (request.getJobOfferWageCAD() == null || request.getJobOfferWageCAD() <= 0) {
                return Optional.of("Valid job offer wage is required when job offer is indicated");
            }
        }
        
        // Validate settlement funds
        if (request.getSettlementFundsCAD() != null && request.getSettlementFundsCAD() < 0) {
            return Optional.of("Settlement funds cannot be negative");
        }
        
        // Validate work experience consistency
        if (request.getForeignWorkExperienceYears() != null && request.getForeignWorkExperienceYears() > 0) {
            if (request.getNocCodeForeign() == null || request.getNocCodeForeign().trim().isEmpty()) {
                return Optional.of("Foreign NOC code is required when foreign work experience is indicated");
            }
        }
        
        // Validate secondary language test consistency
        if (Boolean.TRUE.equals(request.getTookSecondaryLanguageTest())) {
            if (request.getSecondaryTestType() == null || request.getSecondaryTestType().trim().isEmpty()) {
                return Optional.of("Secondary language test type is required when secondary test is indicated");
            }
            if (request.getSecondaryTestSpeakingScore() == null) {
                return Optional.of("Secondary language speaking score is required when secondary test is indicated");
            }
            if (request.getSecondaryTestListeningScore() == null) {
                return Optional.of("Secondary language listening score is required when secondary test is indicated");
            }
            if (request.getSecondaryTestReadingScore() == null) {
                return Optional.of("Secondary language reading score is required when secondary test is indicated");
            }
            if (request.getSecondaryTestWritingScore() == null) {
                return Optional.of("Secondary language writing score is required when secondary test is indicated");
            }
        }
        
        // Validate partner data consistency with marital status
        if ("Single".equalsIgnoreCase(request.getApplicantMaritalStatus())) {
            if (request.getPartnerEducationLevel() != null && !request.getPartnerEducationLevel().trim().isEmpty()) {
                return Optional.of("Partner education level should not be provided for single applicants");
            }
            if (request.getPartnerLanguageTestType() != null && !request.getPartnerLanguageTestType().trim().isEmpty()) {
                return Optional.of("Partner language test information should not be provided for single applicants");
            }
            if (request.getPartnerTestSpeakingScore() != null || 
                request.getPartnerTestListeningScore() != null ||
                request.getPartnerTestReadingScore() != null ||
                request.getPartnerTestWritingScore() != null) {
                return Optional.of("Partner language scores should not be provided for single applicants");
            }
            // Only validate partner work experience if it's not 0 (0 is effectively the same as null)
            if (request.getPartnerCanadianWorkExperienceYears() != null && 
                request.getPartnerCanadianWorkExperienceYears() > 0) {
                return Optional.of("Partner work experience should not be provided for single applicants");
            }
        }
        
        // Validate provincial nomination consistency
        if (Boolean.TRUE.equals(request.getHasProvincialNomination())) {
            if (request.getProvinceOfInterest() == null || request.getProvinceOfInterest().trim().isEmpty()) {
                return Optional.of("Province of interest is required when provincial nomination is indicated");
            }
        }
        
        // All validations passed
        return Optional.empty();
    }
}
