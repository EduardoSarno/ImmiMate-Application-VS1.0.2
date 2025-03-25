package co.immimate.profile.service;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import co.immimate.profile.dto.ProfileSubmissionRequest;
import co.immimate.profile.dto.ProfileSubmissionResponse;
import co.immimate.profile.model.UserImmigrationProfile;
import co.immimate.profile.repository.UserImmigrationProfileRepository;
import co.immimate.user.model.User;
import co.immimate.user.repository.UserRepository;

/**
 * Unit tests for ProfileService class
 */
@ExtendWith(MockitoExtension.class)
public class ProfileServiceTest {

    @Mock
    private UserImmigrationProfileRepository profileRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private ProfileService profileService;
    
    private User testUser;
    private ProfileSubmissionRequest testRequest;
    
    @BeforeEach
    public void setUp() {
        // Setup a test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setRole("USER");
        
        // Setup a basic profile submission request
        testRequest = new ProfileSubmissionRequest();
        testRequest.setUserEmail("test@example.com");
        testRequest.setApplicantName("Test User");
        testRequest.setApplicantAge(30);
        testRequest.setApplicantCitizenship("Canada");
        testRequest.setApplicantResidence("Canada");
        testRequest.setApplicantMaritalStatus("Single");
        testRequest.setApplicantEducationLevel("bachelor");
        testRequest.setHasEducationalCredentialAssessment(false);
        testRequest.setPrimaryLanguageTestType("IELTS");
        testRequest.setPrimaryTestSpeakingScore(8);
        testRequest.setPrimaryTestListeningScore(8);
        testRequest.setPrimaryTestReadingScore(8);
        testRequest.setPrimaryTestWritingScore(8);
        testRequest.setTookSecondaryLanguageTest(false);
        testRequest.setCanadianWorkExperienceYears(2);
        testRequest.setForeignWorkExperienceYears(0);
        testRequest.setWorkingInCanada(true);
        testRequest.setHasProvincialNomination(false);
        testRequest.setProvinceOfInterest("Ontario");
        testRequest.setHasCanadianRelatives(false);
        testRequest.setReceivedInvitationToApply(false);
        testRequest.setSettlementFundsCAD(12500);
        testRequest.setPreferredCity("Toronto");
        testRequest.setPreferredDestinationProvince("Ontario");
        testRequest.setHasJobOffer(false);
        testRequest.setJsonPayload("{}");
    }
    
    @Test
    void testSubmitProfile_Success() {
        // Setup
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        
        // Mock repository to set an applicationId on the saved profile
        when(profileRepository.save(any(UserImmigrationProfile.class))).thenAnswer(invocation -> {
            UserImmigrationProfile profile = invocation.getArgument(0);
            if (profile.getApplicationId() == null) {
                profile.setApplicationId(UUID.randomUUID()); // Set a UUID if not already set
            }
            return profile;
        });
        
        // Execute
        ProfileSubmissionResponse response = profileService.submitProfile(testRequest);
        
        // Verify
        assertTrue(response.isSuccess());
        assertNotNull(response.getProfileId());
        verify(profileRepository).save(any(UserImmigrationProfile.class));
    }
} 