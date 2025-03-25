package co.immimate.profile;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import co.immimate.profile.dto.ProfileSubmissionRequest;
import co.immimate.profile.dto.ProfileSubmissionResponse;
import co.immimate.profile.model.UserImmigrationProfile;
import co.immimate.profile.repository.UserImmigrationProfileRepository;
import co.immimate.profile.service.ProfileService;
import co.immimate.user.model.User;
import co.immimate.user.repository.UserRepository;

/**
 * Comprehensive test class for ProfileService
 */
@SpringBootTest
@ActiveProfiles("test")
class ProfileServiceTest {

    @Mock
    private UserImmigrationProfileRepository profileRepository;
    
    @Mock
    private UserRepository userRepository;

    
    @InjectMocks
    private ProfileService profileService;

    // Test constants
    private static final UUID TEST_APPLICATION_ID = UUID.fromString("b52cfe1a-cf77-44d6-a384-fa918b9cd3b3");
    private static final UUID TEST_USER_ID = UUID.fromString("a1b2c3d4-e5f6-4321-8765-987654321abc");
    private static final String TEST_EMAIL = "test@example.com";
    
    private User testUser;
    private UserImmigrationProfile testProfile;
    private ProfileSubmissionRequest testRequest;

    public ProfileServiceTest() {
    }
    
    @BeforeEach
    public void setUp() {
        // Create test user
        testUser = new User();
        testUser.setId(TEST_USER_ID);
        testUser.setEmail(TEST_EMAIL);
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setName("Test User");
        testUser.setRole("USER");
        testUser.setCreatedAt(Instant.now());
        testUser.setUpdatedAt(Instant.now());
        
        // Create test profile with complete information
        testProfile = createCompleteTestProfile();
        
        // Create test request
        testRequest = createCompleteProfileRequest();
        
        // Set up repository mocks
        when(profileRepository.findByApplicationId(TEST_APPLICATION_ID)).thenReturn(Optional.of(testProfile));
        when(profileRepository.findFirstByUserEmailOrderByCreatedAtDesc(TEST_EMAIL)).thenReturn(Optional.of(testProfile));
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        
        // Mock the save method to set the applicationId and return the profile
        when(profileRepository.save(any(UserImmigrationProfile.class))).thenAnswer(invocation -> {
            UserImmigrationProfile savedProfile = invocation.getArgument(0);
            if (savedProfile.getApplicationId() == null) {
                savedProfile.setApplicationId(UUID.randomUUID());
            }
            return savedProfile;
        });
        
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            if (savedUser.getId() == null) {
                savedUser.setId(UUID.randomUUID());
            }
            return savedUser;
        });
    }
    
    /**
     * Create a complete test profile with all fields populated
     */
    private UserImmigrationProfile createCompleteTestProfile() {
        UserImmigrationProfile profile = new UserImmigrationProfile();
        
        // Set basic fields
        profile.setApplicationId(TEST_APPLICATION_ID);
        profile.setUser(testUser);
        profile.setUserEmail(TEST_EMAIL);
        profile.setApplicantName("Test Applicant");
        profile.setApplicantAge(32);
        profile.setApplicantCitizenship("Mexico");
        profile.setApplicantResidence("United States");
        profile.setApplicantMaritalStatus("Married");
        
        // Education
        profile.setApplicantEducationLevel("Master's Degree");
        profile.setEducationCompletedInCanada(true);
        profile.setCanadianEducationLevel("Bachelor's Degree");
        profile.setHasEducationalCredentialAssessment(true);
        profile.setTradesCertification(true);
        
        // Language
        profile.setPrimaryLanguageTestType("IELTS");
        profile.setPrimaryTestSpeakingScore(8);
        profile.setPrimaryTestListeningScore(9);
        profile.setPrimaryTestReadingScore(8);
        profile.setPrimaryTestWritingScore(7);
        
        profile.setTookSecondaryLanguageTest(true);
        profile.setSecondaryTestType("TEF");
        profile.setSecondaryTestSpeakingScore(7);
        profile.setSecondaryTestListeningScore(8);
        profile.setSecondaryTestReadingScore(7);
        profile.setSecondaryTestWritingScore(6);
        
        // Work experience
        profile.setCanadianWorkExperienceYears(2);
        profile.setNocCodeCanadian(21223);
        profile.setForeignWorkExperienceYears(5);
        profile.setNocCodeForeign(21223);
        profile.setWorkingInCanada(true);
        
        // Partner information
        profile.setPartnerEducationLevel("Bachelor's Degree");
        profile.setPartnerLanguageTestType("IELTS");
        profile.setPartnerTestSpeakingScore(7);
        profile.setPartnerTestListeningScore(8);
        profile.setPartnerTestReadingScore(7);
        profile.setPartnerTestWritingScore(6);
        profile.setPartnerCanadianWorkExperienceYears(1);
        
        // Provincial information
        profile.setHasProvincialNomination(true);
        profile.setProvinceOfInterest("Ontario");
        profile.setHasCanadianRelatives(true);
        profile.setRelationshipWithCanadianRelative("Sibling");
        profile.setReceivedInvitationToApply(false);
        
        // Job information
        profile.setHasJobOffer(true);
        profile.setIsJobOfferLmiaApproved(true);
        profile.setJobOfferWageCad(75000);
        profile.setJobOfferNocCode(21223);
        
        // Additional information
        profile.setSettlementFundsCad(25000);
        profile.setPreferredCity("Toronto");
        profile.setPreferredDestinationProvince("Ontario");
        
        // JSON payload
        profile.setJsonPayload("{\"additionalData\":\"test\"}");
        
        // Timestamps
        profile.setCreatedAt(Instant.now());
        profile.setLastModifiedAt(Instant.now());
        
        return profile;
    }
    
    /**
     * Create a complete profile submission request
     */
    private ProfileSubmissionRequest createCompleteProfileRequest() {
        ProfileSubmissionRequest request = new ProfileSubmissionRequest();
        
        // Set basic fields
        request.setUserId(TEST_USER_ID);
        request.setUserEmail(TEST_EMAIL);
        request.setApplicantName("Test Applicant");
        request.setApplicantAge(32);
        request.setApplicantCitizenship("Mexico");
        request.setApplicantResidence("United States");
        request.setApplicantMaritalStatus("Married");
        
        // Education
        request.setApplicantEducationLevel("Master's Degree");
        request.setEducationCompletedInCanada(true);
        request.setCanadianEducationLevel("Bachelor's Degree");
        request.setHasEducationalCredentialAssessment(true);
        request.setTradesCertification(true);
        
        // Language
        request.setPrimaryLanguageTestType("IELTS");
        request.setPrimaryTestSpeakingScore(8);
        request.setPrimaryTestListeningScore(9);
        request.setPrimaryTestReadingScore(8);
        request.setPrimaryTestWritingScore(7);
        
        request.setTookSecondaryLanguageTest(true);
        request.setSecondaryTestType("TEF");
        request.setSecondaryTestSpeakingScore(7);
        request.setSecondaryTestListeningScore(8);
        request.setSecondaryTestReadingScore(7);
        request.setSecondaryTestWritingScore(6);
        
        // Work experience
        request.setCanadianWorkExperienceYears(2);
        request.setNocCodeCanadian(21223);
        request.setForeignWorkExperienceYears(5);
        request.setNocCodeForeign(21223);
        request.setWorkingInCanada(true);
        
        // Partner information
        request.setPartnerEducationLevel("Bachelor's Degree");
        request.setPartnerLanguageTestType("IELTS");
        request.setPartnerTestSpeakingScore(7);
        request.setPartnerTestListeningScore(8);
        request.setPartnerTestReadingScore(7);
        request.setPartnerTestWritingScore(6);
        request.setPartnerCanadianWorkExperienceYears(1);
        
        // Provincial information
        request.setHasProvincialNomination(true);
        request.setProvinceOfInterest("Ontario");
        request.setHasCanadianRelatives(true);
        request.setRelationshipWithCanadianRelative("Sibling");
        request.setReceivedInvitationToApply(false);
        
        // Job information
        request.setHasJobOffer(true);
        request.setIsJobOfferLmiaApproved(true);
        request.setJobOfferWageCAD(75000);
        request.setJobOfferNocCode(21223);
        
        // Additional information
        request.setSettlementFundsCAD(25000);
        request.setPreferredCity("Toronto");
        request.setPreferredDestinationProvince("Ontario");
        
        // JSON payload
        request.setJsonPayload("{\"additionalData\":\"test\"}");
        
        return request;
    }

    @Test
    void testGetProfileByApplicationId() {
        // Test retrieving a profile by application ID
        Optional<UserImmigrationProfile> result = profileService.getProfileByApplicationId(TEST_APPLICATION_ID);
        
        // Verify the repository was called
        verify(profileRepository).findByApplicationId(TEST_APPLICATION_ID);
        
        // Verify the result
        assertTrue(result.isPresent(), "Profile should be found");
        UserImmigrationProfile profile = result.get();
        
        // Verify basic fields
        assertEquals(TEST_APPLICATION_ID, profile.getApplicationId());
        assertEquals(TEST_EMAIL, profile.getUserEmail());
        assertEquals("Test Applicant", profile.getApplicantName());
        assertEquals(32, profile.getApplicantAge());
        assertEquals("Mexico", profile.getApplicantCitizenship());
        assertEquals("United States", profile.getApplicantResidence());
        assertEquals("Married", profile.getApplicantMaritalStatus());
        
        // Verify education fields
        assertEquals("Master's Degree", profile.getApplicantEducationLevel());
        assertTrue(profile.getEducationCompletedInCanada());
        assertEquals("Bachelor's Degree", profile.getCanadianEducationLevel());
        assertTrue(profile.isHasEducationalCredentialAssessment());
        assertTrue(profile.getTradesCertification());
        
        // Verify language fields
        assertEquals("IELTS", profile.getPrimaryLanguageTestType());
        assertEquals(8, profile.getPrimaryTestSpeakingScore());
        assertEquals(9, profile.getPrimaryTestListeningScore());
        assertEquals(8, profile.getPrimaryTestReadingScore());
        assertEquals(7, profile.getPrimaryTestWritingScore());
        
        assertTrue(profile.isTookSecondaryLanguageTest());
        assertEquals("TEF", profile.getSecondaryTestType());
        assertEquals(Integer.valueOf(7), profile.getSecondaryTestSpeakingScore());
        assertEquals(Integer.valueOf(8), profile.getSecondaryTestListeningScore());
        assertEquals(Integer.valueOf(7), profile.getSecondaryTestReadingScore());
        assertEquals(Integer.valueOf(6), profile.getSecondaryTestWritingScore());
        
        // Verify work experience fields
        assertEquals(2, profile.getCanadianWorkExperienceYears());
        assertEquals(Integer.valueOf(21223), profile.getNocCodeCanadian());
        assertEquals(5, profile.getForeignWorkExperienceYears());
        assertEquals(Integer.valueOf(21223), profile.getNocCodeForeign());
        assertTrue(profile.isWorkingInCanada());
        
        // Verify partner information
        assertEquals("Bachelor's Degree", profile.getPartnerEducationLevel());
        assertEquals("IELTS", profile.getPartnerLanguageTestType());
        assertEquals(Integer.valueOf(7), profile.getPartnerTestSpeakingScore());
        assertEquals(Integer.valueOf(8), profile.getPartnerTestListeningScore());
        assertEquals(Integer.valueOf(7), profile.getPartnerTestReadingScore());
        assertEquals(Integer.valueOf(6), profile.getPartnerTestWritingScore());
        assertEquals(Integer.valueOf(1), profile.getPartnerCanadianWorkExperienceYears());
        
        // Verify provincial information
        assertTrue(profile.isHasProvincialNomination());
        assertEquals("Ontario", profile.getProvinceOfInterest());
        assertTrue(profile.isHasCanadianRelatives());
        assertEquals("Sibling", profile.getRelationshipWithCanadianRelative());
        assertFalse(profile.isReceivedInvitationToApply());
        
        // Verify job information
        assertTrue(profile.isHasJobOffer());
        assertTrue(profile.getIsJobOfferLmiaApproved());
        assertEquals(Integer.valueOf(75000), profile.getJobOfferWageCad());
        assertEquals(Integer.valueOf(21223), profile.getJobOfferNocCode());
        
        // Verify additional information
        assertEquals(25000, profile.getSettlementFundsCad());
        assertEquals("Toronto", profile.getPreferredCity());
        assertEquals("Ontario", profile.getPreferredDestinationProvince());
        
        // Verify JSON payload
        assertEquals("{\"additionalData\":\"test\"}", profile.getJsonPayload());
        
        // Verify user relationship
        assertNotNull(profile.getUser());
        assertEquals(TEST_USER_ID, profile.getUser().getId());
        assertEquals(TEST_EMAIL, profile.getUser().getEmail());
    }
    
    @Test
    void testGetMostRecentProfileByEmail() {
        // Test retrieving the most recent profile by email
        Optional<UserImmigrationProfile> result = profileService.getMostRecentProfileByEmail(TEST_EMAIL);
        
        // Verify the repository was called
        verify(profileRepository).findFirstByUserEmailOrderByCreatedAtDesc(TEST_EMAIL);
        
        // Verify the result
        assertTrue(result.isPresent(), "Profile should be found");
        assertEquals(TEST_APPLICATION_ID, result.get().getApplicationId());
    }
    
    @Test
    void testSubmitProfileWithExistingUser() {
        // Test submitting a profile for an existing user
        ProfileSubmissionResponse response = profileService.submitProfile(testRequest);
        
        // Verify the user repository was called to find the user
        verify(userRepository).findById(TEST_USER_ID);
        
        // Verify the profile repository was called to save the profile
        verify(profileRepository).save(any(UserImmigrationProfile.class));
        
        // Verify the response
        assertTrue(response.isSuccess());
        assertNotNull(response.getProfileId());
        assertEquals("Profile submitted successfully", response.getMessage());
    }

    @Test
    void testSubmitProfileWithNewUser() {
        // Set up a request with a non-existent user
        ProfileSubmissionRequest newUserRequest = createCompleteProfileRequest();
        newUserRequest.setUserId(UUID.randomUUID()); // Use a different user ID
        newUserRequest.setUserEmail("newuser@example.com");
        
        // Mock the user repository to return empty for both ID and email
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        
        // Mock the user repository to return a new user when saved
        User newUser = new User();
        newUser.setId(UUID.randomUUID());
        newUser.setEmail("newuser@example.com");
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        
        // Test submitting a profile for a new user
        ProfileSubmissionResponse response = profileService.submitProfile(newUserRequest);
        
        // Verify the user repository was called to find and then save the user
        verify(userRepository).findById(any(UUID.class));
        verify(userRepository).findByEmail("newuser@example.com");
        verify(userRepository).save(any(User.class));
        
        // Verify the profile repository was called to save the profile
        verify(profileRepository).save(any(UserImmigrationProfile.class));
        
        // Verify the response
        assertTrue(response.isSuccess());
        assertNotNull(response.getProfileId());
        assertEquals("Profile submitted successfully", response.getMessage());
    }

    @Test
    void testSubmitProfileWithInvalidJsonPayload() {
        // Set up a request with an invalid JSON payload
        ProfileSubmissionRequest invalidRequest = createCompleteProfileRequest();
        invalidRequest.setJsonPayload("This is not valid JSON");
        
        // Test submitting a profile with an invalid JSON payload
        ProfileSubmissionResponse response = profileService.submitProfile(invalidRequest);
        
        // Verify the response is still successful (service should handle the error)
        assertTrue(response.isSuccess());
        
        // Verify the profile repository was called to save the profile
        verify(profileRepository).save(any(UserImmigrationProfile.class));
    }

    @Test
    void testSubmitProfileWithRepositoryException() {
        // Mock the profile repository to throw an exception when saving
        when(profileRepository.save(any(UserImmigrationProfile.class))).thenThrow(new RuntimeException("Test exception"));
        
        // Test submitting a profile with a repository exception
        ProfileSubmissionResponse response = profileService.submitProfile(testRequest);
        
        // Verify the response
        assertFalse(response.isSuccess());
        assertNull(response.getProfileId());
        assertTrue(response.getMessage().contains("Error saving profile"));
    }

    @Test
    void testSubmitProfileWithMissingRequiredFields() {
        // Create a complete request, then set key fields to null or empty
        ProfileSubmissionRequest request = createCompleteProfileRequest();
        request.setApplicantName(null);  // Missing applicant name

        // Act
        ProfileSubmissionResponse response = profileService.submitProfile(request);

        // Assert
        assertFalse(response.isSuccess(), "Response should indicate failure for missing fields");
        assertTrue(
                response.getMessage().toLowerCase().contains("applicant name"),
                "Error message should mention the missing applicantName field"
        );
    }

    @Test
    void testSubmitProfileWithInvalidAge() {
        // Create a request with an unrealistic or negative age
        ProfileSubmissionRequest request = createCompleteProfileRequest();
        request.setApplicantAge(-5); // Invalid age

        // Act
        ProfileSubmissionResponse response = profileService.submitProfile(request);

        // Assert
        assertFalse(response.isSuccess(), "Response should indicate failure for invalid age");
        assertTrue(
                response.getMessage().toLowerCase().contains("age"),
                "Error message should mention the invalid age"
        );
    }

    @Test
    void testSubmitProfileWithInvalidLanguageScores() {
        // Create a request with out-of-range language score
        ProfileSubmissionRequest request = createCompleteProfileRequest();
        request.setPrimaryTestReadingScore(15); // Example invalid range

        // Act
        ProfileSubmissionResponse response = profileService.submitProfile(request);

        // Assert
        assertFalse(response.isSuccess(), "Profile submission should fail for invalid language scores");
        assertTrue(
                response.getMessage().toLowerCase().contains("reading score"),
                "Error message should mention the invalid reading score"
        );
    }

    @Test
    void testSubmitProfileWithJobOfferConsistency() {
        // Create a request indicating there's a job offer, but missing relevant fields
        ProfileSubmissionRequest request = createCompleteProfileRequest();
        request.setHasJobOffer(true);
        request.setJobOfferNocCode(null); // Missing NOC code
        request.setJobOfferWageCAD(null); // Missing job offer wage

        // Act
        ProfileSubmissionResponse response = profileService.submitProfile(request);

        // Assert
        assertFalse(response.isSuccess(), "Submission should fail when job offer data is incomplete");
        assertTrue(
                response.getMessage().toLowerCase().contains("job offer"),
                "Error message should point to invalid or missing job offer details"
        );
    }

    @Test
    void testSubmitProfileWithNegativeSettlementFunds() {
        // Create a request with negative settlement funds
        ProfileSubmissionRequest request = createCompleteProfileRequest();
        request.setSettlementFundsCAD(-1000);

        // Act
        ProfileSubmissionResponse response = profileService.submitProfile(request);

        // Assert
        assertFalse(response.isSuccess(), "Submission should fail for negative settlement funds");
        assertTrue(
                response.getMessage().toLowerCase().contains("settlement funds"),
                "Error message should mention invalid (negative) settlement funds"
        );
    }

    @Test
    void testSubmitProfileWithInvalidWorkExperience() {
        // Create a request indicating foreign work experience, but missing NOC code
        ProfileSubmissionRequest request = createCompleteProfileRequest();
        request.setForeignWorkExperienceYears(3);
        request.setNocCodeForeign(null); // Should fail due to missing NOC code

        // Act
        ProfileSubmissionResponse response = profileService.submitProfile(request);

        // Assert
        assertFalse(response.isSuccess(), "Submission should fail for inconsistent work experience data");
        assertTrue(
                response.getMessage().toLowerCase().contains("noc code"),
                "Error message should mention missing foreign NOC code"
        );
    }

    @Test
    void testSubmitProfileWithSecondaryLanguageConsistency() {
        // Create a request indicating a secondary language test, but missing secondary scores
        ProfileSubmissionRequest request = createCompleteProfileRequest();
        request.setTookSecondaryLanguageTest(true);
        request.setSecondaryTestType("CELPIP"); // Changed from existing setup
        request.setSecondaryTestSpeakingScore(null);  // Missing data

        // Act
        ProfileSubmissionResponse response = profileService.submitProfile(request);

        // Assert
        assertFalse(response.isSuccess(), "Profile submission should fail for incomplete secondary language test info");
        assertTrue(
                response.getMessage().toLowerCase().contains("secondary language"),
                "Error message should mention invalid or missing secondary language scores"
        );
    }

    @Test
    void testSubmitProfileWithPartnerDataMismatch() {
        // Create a request with single marital status but partner details filled
        ProfileSubmissionRequest request = createCompleteProfileRequest();
        request.setApplicantMaritalStatus("Single");
        request.setPartnerEducationLevel("Bachelor's Degree"); // Shouldn't exist if user is single

        // Act
        ProfileSubmissionResponse response = profileService.submitProfile(request);

        // Assert
        assertFalse(response.isSuccess(), "Submission should fail if partner data is provided for a single applicant");
        assertTrue(
                response.getMessage().toLowerCase().contains("partner"),
                "Error message should indicate mismatch between marital status and partner info"
        );
    }

    @Test
    void testSubmitProfileWithProvincialNominationConsistency() {
        // Create a request with provincial nomination but no province specified
        ProfileSubmissionRequest request = createCompleteProfileRequest();
        request.setHasProvincialNomination(true);
        request.setProvinceOfInterest(null); // Missing province despite having nomination

        // Act
        ProfileSubmissionResponse response = profileService.submitProfile(request);

        // Assert
        assertFalse(response.isSuccess(), "Submission should fail when provincial nomination exists without a province");
        assertTrue(
                response.getMessage().toLowerCase().contains("province"),
                "Error message should mention missing province information"
        );
    }

    @Test
    void testBulkInsertProfiles() {
        // Setup: Define a smaller number for testing (to avoid long-running tests)
        int numProfiles = 10; // Reduced from 1000 for testing purposes
        boolean allSucceeded = true;
        UUID lastProfileId = null;

        // Act: Create multiple profiles
        for (int i = 0; i < numProfiles; i++) {
            ProfileSubmissionRequest request = createCompleteProfileRequest();
            // Vary some data to simulate real usage
            request.setApplicantName("TestUser" + i);
            request.setUserEmail("test" + i + "@example.com");
            
            ProfileSubmissionResponse response = profileService.submitProfile(request);
            if (!response.isSuccess()) {
                allSucceeded = false;
                break;
            }
            lastProfileId = response.getProfileId();
        }

        // Assert
        assertTrue(allSucceeded, "All bulk insert profile submissions should succeed");
        assertNotNull(lastProfileId, "Last profile ID should not be null");
        
        // Verify the saveAll method was called multiple times
        verify(profileRepository, org.mockito.Mockito.atLeast(numProfiles)).save(any(UserImmigrationProfile.class));
    }
}