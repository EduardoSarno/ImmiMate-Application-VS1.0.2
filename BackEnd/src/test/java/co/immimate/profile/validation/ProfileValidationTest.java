package co.immimate.profile.validation;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import co.immimate.profile.dto.ProfileSubmissionRequest;
import co.immimate.profile.dto.ProfileSubmissionResponse;
import co.immimate.profile.model.UserImmigrationProfile;
import co.immimate.profile.service.ProfileService;
import co.immimate.user.model.User;

/**
 * Comprehensive Test Suite for Profile Form Validation
 * 
 * This test class focuses on validating form input and ensuring data consistency
 * when users change values that should affect other fields.
 */
@ExtendWith(MockitoExtension.class)
public class ProfileValidationTest {
    
    @InjectMocks
    private ProfileService profileService;
    
    private User testUser;
    private ProfileSubmissionRequest baseRequest;
    
    @BeforeEach
    public void setUp() {
        // Create a test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        
        // Create a base profile submission request with all required fields
        baseRequest = new ProfileSubmissionRequest();
        baseRequest.setUserEmail("test@example.com");
        baseRequest.setApplicantName("Test User");
        baseRequest.setApplicantAge(30);
        baseRequest.setApplicantCitizenship("Canada");
        baseRequest.setApplicantResidence("Canada");
        baseRequest.setApplicantMaritalStatus("Single");
        baseRequest.setApplicantEducationLevel("bachelors-degree");
        baseRequest.setEducationCompletedInCanada(false);
        baseRequest.setHasEducationalCredentialAssessment(false);
        baseRequest.setPrimaryLanguageTestType("IELTS");
        baseRequest.setPrimaryTestSpeakingScore(8);
        baseRequest.setPrimaryTestListeningScore(8);
        baseRequest.setPrimaryTestReadingScore(8);
        baseRequest.setPrimaryTestWritingScore(8);
        baseRequest.setTookSecondaryLanguageTest(false);
        baseRequest.setCanadianWorkExperienceYears(2);
        baseRequest.setNocCodeCanadian(50010);
        baseRequest.setForeignWorkExperienceYears(0);
        baseRequest.setWorkingInCanada(true);
        baseRequest.setHasProvincialNomination(false);
        baseRequest.setProvinceOfInterest("Ontario");
        baseRequest.setHasCanadianRelatives(false);
        baseRequest.setReceivedInvitationToApply(false);
        baseRequest.setSettlementFundsCAD(12500);
        baseRequest.setPreferredCity("Toronto");
        baseRequest.setPreferredDestinationProvince("Ontario");
        baseRequest.setHasJobOffer(false);
        baseRequest.setTradesCertification(false);
    }
    
    /**
     * Test 1: Changing marital status mid-form and leaving stale spouse data
     * Verifies that when marital status is 'Single', spouse fields are properly handled
     */
    @Test
    void testMaritalStatus_SingleWithPartnerData() {
        // Arrange: Create a request with Single marital status but with partner data
        ProfileSubmissionRequest request = cloneBaseRequest();
        request.setApplicantMaritalStatus("Single");
        request.setPartnerEducationLevel("bachelors-degree");
        request.setPartnerLanguageTestType("IELTS");
        request.setPartnerTestSpeakingScore(7);
        request.setPartnerTestListeningScore(7);
        request.setPartnerTestReadingScore(7);
        request.setPartnerTestWritingScore(7);
        request.setPartnerCanadianWorkExperienceYears(1);
        
        // Act
        ProfileSubmissionResponse response = profileService.submitProfile(request);
        
        // Assert: Should detect contradiction and return validation error
        assertFalse(response.isSuccess(), "Should reject Single status with partner data");
    }
    
    /**
     * Test 2: Switching job offer from "No" to "Yes" with job data
     */
    @Test
    public void testJobOffer_NoWithJobData() {
        // Clone the base request
        ProfileSubmissionRequest request = cloneBaseRequest();
        
        // Set hasJobOffer to false but provide job offer data
        request.setHasJobOffer(false);
        request.setIsJobOfferLmiaApproved(true);
        request.setJobOfferWageCAD(75000);
        request.setJobOfferNocCode(10010);
        
        // Create a new UserImmigrationProfile
        UserImmigrationProfile profile = new UserImmigrationProfile();
        
        // Get the setJobOfferInfo method via reflection
        Method setJobOfferInfoMethod;
        try {
            setJobOfferInfoMethod = ProfileService.class.getDeclaredMethod("setJobOfferInfo", UserImmigrationProfile.class, ProfileSubmissionRequest.class);
            setJobOfferInfoMethod.setAccessible(true);
            
            // Call the method directly
            setJobOfferInfoMethod.invoke(profileService, profile, request);
            
            // Verify the job offer fields are nullified
            assertNull(profile.getJobOfferNocCode(), "Job offer NOC code should be nullified");
            assertNull(profile.getIsJobOfferLmiaApproved(), "Job offer LMIA approved should be nullified");
            assertNull(profile.getJobOfferWageCad(), "Job offer wage should be nullified");
            
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            fail("Failed to invoke setJobOfferInfo method via reflection: " + e.getMessage());
        }
    }
    
    /**
     * Test 3: Switching secondary language from "Yes" to "No" with secondary language data
     */
    @Test
    public void testSecondaryLanguage_NoWithScores() {
        // Clone the base request
        ProfileSubmissionRequest request = cloneBaseRequest();
        
        // Set tookSecondaryLanguageTest to false but provide secondary language scores
        request.setTookSecondaryLanguageTest(false);
        request.setSecondaryTestType("TEF");
        request.setSecondaryTestSpeakingScore(7);
        request.setSecondaryTestListeningScore(7);
        request.setSecondaryTestReadingScore(7);
        request.setSecondaryTestWritingScore(7);
        
        // Create a new UserImmigrationProfile
        UserImmigrationProfile profile = new UserImmigrationProfile();
        
        // Get the setLanguageInfo method via reflection
        Method setLanguageInfoMethod;
        try {
            setLanguageInfoMethod = ProfileService.class.getDeclaredMethod("setLanguageInfo", UserImmigrationProfile.class, ProfileSubmissionRequest.class);
            setLanguageInfoMethod.setAccessible(true);
            
            // Call the method directly
            setLanguageInfoMethod.invoke(profileService, profile, request);
            
            // Verify the secondary language fields are nullified
            assertNull(profile.getSecondaryTestType(), "Secondary test type should be nullified");
            assertNull(profile.getSecondaryTestSpeakingScore(), "Secondary speaking score should be nullified");
            assertNull(profile.getSecondaryTestListeningScore(), "Secondary listening score should be nullified");
            assertNull(profile.getSecondaryTestReadingScore(), "Secondary reading score should be nullified");
            assertNull(profile.getSecondaryTestWritingScore(), "Secondary writing score should be nullified");
            
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            fail("Failed to invoke setLanguageInfo method via reflection: " + e.getMessage());
        }
    }
    
    /**
     * Test 4: User enters contradictory data - Canadian education but no education level
     */
    @Test
    void testContradictoryData_CanadianEducationNoLevel() {
        // Arrange: Create request with Canadian education true but no level
        ProfileSubmissionRequest request = cloneBaseRequest();
        request.setEducationCompletedInCanada(true);
        request.setCanadianEducationLevel(null);
        
        // Act: Submit the profile
        ProfileSubmissionResponse response = profileService.submitProfile(request);
        
        // Assert: Should fail validation
        assertFalse(response.isSuccess(), "Should reject contradictory Canadian education data");
        assertTrue(response.getMessage().contains("Canadian education level"), 
                "Error should mention Canadian education level");
    }
    
    /**
     * Test 5: User enters invalid negative age
     */
    @Test
    void testInvalidNumericFields_NegativeAge() {
        // Arrange: Create request with negative age
        ProfileSubmissionRequest request = cloneBaseRequest();
        request.setApplicantAge(-5);
        
        // Act: Submit the profile
        ProfileSubmissionResponse response = profileService.submitProfile(request);
        
        // Assert: Should fail validation
        assertFalse(response.isSuccess(), "Should reject negative age");
        assertTrue(response.getMessage().contains("age"), 
                "Error should mention age field");
    }
    
    /**
     * Test 6: User enters negative settlement funds
     */
    @Test
    void testInvalidNumericFields_NegativeSettlementFunds() {
        // Arrange: Create request with negative settlement funds
        ProfileSubmissionRequest request = cloneBaseRequest();
        request.setSettlementFundsCAD(-5000);
        
        // Act: Submit the profile
        ProfileSubmissionResponse response = profileService.submitProfile(request);
        
        // Assert: Should fail validation
        assertFalse(response.isSuccess(), "Should reject negative settlement funds");
    }
    
    /**
     * Test 7: User has foreign work experience but no NOC code
     */
    @Test
    void testWorkExperience_ForeignWithoutNocCode() {
        // Clone the base request
        ProfileSubmissionRequest request = cloneBaseRequest();
        
        // Set foreign work experience but no NOC code
        request.setForeignWorkExperienceYears(3);
        request.setNocCodeForeign(null);
        
        // Submit the profile
        ProfileSubmissionResponse response = profileService.submitProfile(request);
        
        // Verify the response is an error
        assertFalse(response.isSuccess(), "Should reject foreign work experience without NOC code");
    }
    
    /**
     * Test 8: User has job offer but no NOC code
     */
    @Test
    void testJobOffer_YesWithoutNocCode() {
        // Clone the base request
        ProfileSubmissionRequest request = cloneBaseRequest();
        
        // Set job offer to true but no NOC code
        request.setHasJobOffer(true);
        request.setJobOfferNocCode(null);
        
        // Submit the profile
        ProfileSubmissionResponse response = profileService.submitProfile(request);
        
        // Verify the response is an error
        assertFalse(response.isSuccess(), "Should reject job offer without NOC code");
    }
    
    /**
     * Test 9: User has provincial nomination but no province of interest
     */
    @Test
    void testProvincialNomination_WithoutProvince() {
        // Clone the base request
        ProfileSubmissionRequest request = cloneBaseRequest();
        
        // Set provincial nomination to true but no province
        request.setHasProvincialNomination(true);
        request.setProvinceOfInterest(null);
        
        // Submit the profile
        ProfileSubmissionResponse response = profileService.submitProfile(request);
        
        // Verify the response is an error
        assertFalse(response.isSuccess(), "Should reject provincial nomination without province");
    }
    
    /**
     * Test 10: User claims Canadian relatives but doesn't specify relationship
     */
    @Test
    void testCanadianRelatives_WithoutRelationship() {
        // Clone the base request
        ProfileSubmissionRequest request = cloneBaseRequest();
        
        // Set Canadian relatives to true but no relationship
        request.setHasCanadianRelatives(true);
        request.setRelationshipWithCanadianRelative(null);
        
        // Submit the profile
        ProfileSubmissionResponse response = profileService.submitProfile(request);
        
        // Verify the response is an error
        assertFalse(response.isSuccess(), "Should reject Canadian relatives without relationship");
    }
    
    /**
     * Helper method to clone the base request for test cases
     */
    private ProfileSubmissionRequest cloneBaseRequest() {
        ProfileSubmissionRequest clone = new ProfileSubmissionRequest();
        
        // Copy all properties from baseRequest to clone
        clone.setUserEmail(baseRequest.getUserEmail());
        clone.setApplicantName(baseRequest.getApplicantName());
        clone.setApplicantAge(baseRequest.getApplicantAge());
        clone.setApplicantCitizenship(baseRequest.getApplicantCitizenship());
        clone.setApplicantResidence(baseRequest.getApplicantResidence());
        clone.setApplicantMaritalStatus(baseRequest.getApplicantMaritalStatus());
        clone.setApplicantEducationLevel(baseRequest.getApplicantEducationLevel());
        clone.setEducationCompletedInCanada(baseRequest.getEducationCompletedInCanada());
        clone.setCanadianEducationLevel(baseRequest.getCanadianEducationLevel());
        clone.setHasEducationalCredentialAssessment(baseRequest.getHasEducationalCredentialAssessment());
        clone.setTradesCertification(baseRequest.getTradesCertification());
        clone.setPrimaryLanguageTestType(baseRequest.getPrimaryLanguageTestType());
        clone.setPrimaryTestSpeakingScore(baseRequest.getPrimaryTestSpeakingScore());
        clone.setPrimaryTestListeningScore(baseRequest.getPrimaryTestListeningScore());
        clone.setPrimaryTestReadingScore(baseRequest.getPrimaryTestReadingScore());
        clone.setPrimaryTestWritingScore(baseRequest.getPrimaryTestWritingScore());
        clone.setTookSecondaryLanguageTest(baseRequest.getTookSecondaryLanguageTest());
        clone.setSecondaryTestType(baseRequest.getSecondaryTestType());
        clone.setSecondaryTestSpeakingScore(baseRequest.getSecondaryTestSpeakingScore());
        clone.setSecondaryTestListeningScore(baseRequest.getSecondaryTestListeningScore());
        clone.setSecondaryTestReadingScore(baseRequest.getSecondaryTestReadingScore());
        clone.setSecondaryTestWritingScore(baseRequest.getSecondaryTestWritingScore());
        clone.setCanadianWorkExperienceYears(baseRequest.getCanadianWorkExperienceYears());
        clone.setNocCodeCanadian(baseRequest.getNocCodeCanadian());
        clone.setForeignWorkExperienceYears(baseRequest.getForeignWorkExperienceYears());
        clone.setNocCodeForeign(baseRequest.getNocCodeForeign());
        clone.setWorkingInCanada(baseRequest.getWorkingInCanada());
        clone.setPartnerEducationLevel(baseRequest.getPartnerEducationLevel());
        clone.setPartnerLanguageTestType(baseRequest.getPartnerLanguageTestType());
        clone.setPartnerTestSpeakingScore(baseRequest.getPartnerTestSpeakingScore());
        clone.setPartnerTestListeningScore(baseRequest.getPartnerTestListeningScore());
        clone.setPartnerTestReadingScore(baseRequest.getPartnerTestReadingScore());
        clone.setPartnerTestWritingScore(baseRequest.getPartnerTestWritingScore());
        clone.setPartnerCanadianWorkExperienceYears(baseRequest.getPartnerCanadianWorkExperienceYears());
        clone.setHasJobOffer(baseRequest.getHasJobOffer());
        clone.setIsJobOfferLmiaApproved(baseRequest.getIsJobOfferLmiaApproved());
        clone.setJobOfferWageCAD(baseRequest.getJobOfferWageCAD());
        clone.setJobOfferNocCode(baseRequest.getJobOfferNocCode());
        clone.setHasProvincialNomination(baseRequest.getHasProvincialNomination());
        clone.setProvinceOfInterest(baseRequest.getProvinceOfInterest());
        clone.setHasCanadianRelatives(baseRequest.getHasCanadianRelatives());
        clone.setRelationshipWithCanadianRelative(baseRequest.getRelationshipWithCanadianRelative());
        clone.setReceivedInvitationToApply(baseRequest.getReceivedInvitationToApply());
        clone.setSettlementFundsCAD(baseRequest.getSettlementFundsCAD());
        clone.setPreferredCity(baseRequest.getPreferredCity());
        clone.setPreferredDestinationProvince(baseRequest.getPreferredDestinationProvince());
        clone.setJsonPayload(baseRequest.getJsonPayload());
        
        return clone;
    }
} 