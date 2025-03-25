package co.immimate.scoringevaluations.calculation.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import co.immimate.profile.model.UserImmigrationProfile;
import co.immimate.profile.service.ProfileService;
import co.immimate.scoringevaluations.evaluation.model.Evaluation;
import co.immimate.scoringevaluations.evaluation.model.EvaluationCategory;
import co.immimate.scoringevaluations.evaluation.model.EvaluationField;
import co.immimate.scoringevaluations.evaluation.model.EvaluationSubcategory;
import co.immimate.scoringevaluations.evaluation.repository.EvaluationCategoryRepository;
import co.immimate.scoringevaluations.evaluation.repository.EvaluationFieldRepository;
import co.immimate.scoringevaluations.evaluation.repository.EvaluationRepository;
import co.immimate.scoringevaluations.evaluation.repository.EvaluationSubcategoryRepository;
import co.immimate.scoringevaluations.grid.model.Grid;
import co.immimate.scoringevaluations.grid.repository.GridFieldRepository;
import co.immimate.scoringevaluations.grid.repository.GridRepository;

/**
 * This test class specifically focuses on testing the cap enforcement logic
 * by creating scenarios where scores would exceed the caps without proper enforcement.
 */
@SpringBootTest
@ActiveProfiles("test")
public class CapsEnforcementTest {

    @Autowired
    private EvaluationService evaluationService;
    
    @Autowired
    private EvaluationRepository evaluationRepository;
    
    @Autowired
    private EvaluationCategoryRepository categoryRepository;
    
    @Autowired
    private EvaluationSubcategoryRepository subcategoryRepository;
    
    @Autowired
    private EvaluationFieldRepository fieldRepository;
    
    @Autowired
    private GridRepository gridRepository;
    
    @Autowired
    @SuppressWarnings("unused")
    private GridFieldRepository gridFieldRepository;
    
    @MockBean
    private ProfileService profileService;
    
    private static final UUID TEST_APPLICATION_ID = UUID.fromString("7c75c62d-4d57-43e9-bfd5-a497c86c2c4d");
    private static final String TEST_GRID_NAME = "Comprehensive Ranking System (CRS)";
    
    @BeforeEach
    public void setup() {
        // Create a mock user profile that would produce scores exceeding caps
        UserImmigrationProfile mockProfile = new UserImmigrationProfile();
        
        // Set extreme values that would result in maximum points for all fields
        mockProfile.setApplicantAge(30); // Optimal age for maximum points
        mockProfile.setApplicantEducationLevel("doctoral-degree"); // PhD - highest education level
        
        // Set maximum language scores
        mockProfile.setPrimaryLanguageTestType("celpip"); 
        mockProfile.setPrimaryTestSpeakingScore(12); // Perfect scores
        mockProfile.setPrimaryTestWritingScore(12);
        mockProfile.setPrimaryTestReadingScore(12);
        mockProfile.setPrimaryTestListeningScore(12);
        
        mockProfile.setSecondaryTestType("tef");
        mockProfile.setSecondaryTestSpeakingScore(12); // Perfect scores
        mockProfile.setSecondaryTestWritingScore(12);
        mockProfile.setSecondaryTestReadingScore(12);
        mockProfile.setSecondaryTestListeningScore(12);
        
        // Set maximum work experience
        mockProfile.setCanadianWorkExperienceYears(5); // Maximum Canadian work exp
        mockProfile.setForeignWorkExperienceYears(10); // Very high foreign work exp
        
        // Set spouse with maximum scores too
        mockProfile.setApplicantMaritalStatus("married-or-common-law"); // Has spouse
        mockProfile.setPartnerEducationLevel("doctoral-degree");
        mockProfile.setPartnerLanguageTestType("celpip");
        mockProfile.setPartnerTestSpeakingScore(12); // Perfect scores
        mockProfile.setPartnerTestWritingScore(12);
        mockProfile.setPartnerTestReadingScore(12);
        mockProfile.setPartnerTestListeningScore(12);
        mockProfile.setPartnerCanadianWorkExperienceYears(5);
        
        // Additional points factors
        mockProfile.setHasCanadianRelatives(true); // For sibling in Canada
        mockProfile.setRelationshipWithCanadianRelative("sibling");
        // French proficiency is represented by secondary language test if French
        mockProfile.setEducationCompletedInCanada(true); // Post-secondary education in Canada
        mockProfile.setHasProvincialNomination(true);
        mockProfile.setHasJobOffer(true);
        mockProfile.setIsJobOfferLmiaApproved(true);
        
        // Additional required fields
        mockProfile.setUserEmail("test@example.com");
        mockProfile.setApplicantName("Test Applicant");
        mockProfile.setApplicantCitizenship("Canada");
        mockProfile.setApplicantResidence("Canada");
        mockProfile.setHasEducationalCredentialAssessment(true);
        mockProfile.setTookSecondaryLanguageTest(true);
        mockProfile.setNocCodeCanadian(12345);
        mockProfile.setWorkingInCanada(true);
        mockProfile.setProvinceOfInterest("Ontario");
        mockProfile.setReceivedInvitationToApply(false);
        mockProfile.setSettlementFundsCad(20000);
        mockProfile.setPreferredCity("Toronto");
        mockProfile.setPreferredDestinationProvince("Ontario");
        mockProfile.setJsonPayload("{}");
        
        // Mock the profile service to return our extreme profile
        when(profileService.getProfileByApplicationId(any(UUID.class))).thenReturn(Optional.of(mockProfile));
    }
    
    @Test
    @DisplayName("Test cap enforcement with profile that would exceed caps")
    public void testCapEnforcement() {
        System.out.println("============== CAP ENFORCEMENT TEST ==============");
        
        // Get the grid
        Grid grid = gridRepository.findByGridName(TEST_GRID_NAME).orElse(null);
        assertNotNull(grid, "Grid should exist: " + TEST_GRID_NAME);
        
        // Generate variables from our mocked profile
        Map<String, Object> userVariables = evaluationService.getUserVariables(TEST_APPLICATION_ID);
        
        // Force extreme values for skill transferability factors to ensure they would exceed caps
        // Create a modified variables map to push scores beyond caps
        Map<String, Object> extremeVariables = new HashMap<>(userVariables);
        
        // Make sure all CLB scores are maximum
        extremeVariables.put("primary_clb_score", 12);
        extremeVariables.put("secondary_clb_score", 12);
        extremeVariables.put("partner_clb_score", 12);
        
        // Execute the evaluation with our extreme variables
        Evaluation evaluation = evaluationService.createEvaluation(
            TEST_APPLICATION_ID, 
            TEST_GRID_NAME, 
            extremeVariables, 
            true // Force spouse to be true for maximum spouse points
        );
        
        evaluation.setNotes("Cap enforcement test - Extreme values");
        evaluationRepository.save(evaluation);
        
        System.out.println("Evaluation created with ID: " + evaluation.getEvaluationId());
        System.out.println("Total score: " + evaluation.getTotalScore());
        
        // Get all categories to verify caps
        List<EvaluationCategory> categories = categoryRepository.findByEvaluationId(evaluation.getEvaluationId());
        
        System.out.println("\n============== VERIFYING CAPS ARE ENFORCED ==============");
        
        // Track raw scores vs capped scores for skill transferability
        int rawSkillTransferabilityScore = 0;
        int cappedSkillTransferabilityScore = 0;
        
        for (EvaluationCategory category : categories) {
            System.out.println("Category: " + category.getCategoryName() + 
                " - Score: " + category.getUserScore() + 
                " / Max: " + category.getMaxPossibleScore());
            
            // Verify no category exceeds its cap
            assertTrue(category.getUserScore() <= category.getMaxPossibleScore(), 
                "Category score should not exceed cap");
            
            // Get subcategories
            List<EvaluationSubcategory> subcategories = subcategoryRepository.findByCatEvalId(category.getCatEvalId());
            
            // Check if this is the skill transferability category
            if (category.getCategoryName().contains("Skill Transferability")) {
                cappedSkillTransferabilityScore = category.getUserScore();
                
                System.out.println("\n  SKILL TRANSFERABILITY DETAIL:");
                
                // Calculate the raw score total from all subcategories
                for (EvaluationSubcategory subcategory : subcategories) {
                    System.out.println("  Subcategory: " + subcategory.getSubcategoryName() + 
                        " - Score: " + subcategory.getUserScore() + 
                        " / Max: " + subcategory.getMaxPossibleScore());
                    
                    // Verify subcategory doesn't exceed its cap
                    assertTrue(subcategory.getUserScore() <= subcategory.getMaxPossibleScore(), 
                        "Subcategory score should not exceed cap");
                    
                    List<EvaluationField> fields = fieldRepository.findBySubcatEvalId(subcategory.getSubcatEvalId());
                    
                    // Calculate raw field scores for this subcategory
                    int rawSubcategoryScore = 0;
                    int cappedSubcategoryScore = subcategory.getUserScore();
                    
                    for (EvaluationField field : fields) {
                        if (field.getUserQualifies()) {
                            rawSubcategoryScore += field.getUserPointsEarned();
                            System.out.println("    Field: " + field.getFieldName() + 
                                " - Points: " + field.getUserPointsEarned() + 
                                " - Value: " + field.getActualValue() +
                                " - Qualifies: " + field.getUserQualifies());
                        }
                    }
                    
                    // Add to total raw skill transferability score
                    rawSkillTransferabilityScore += rawSubcategoryScore;
                    
                    // Check if capping was applied to this subcategory
                    if (rawSubcategoryScore > subcategory.getMaxPossibleScore()) {
                        System.out.println("    SUBCATEGORY CAP APPLIED: Raw score " + rawSubcategoryScore + 
                            " capped to " + cappedSubcategoryScore);
                        
                        // Extra verification
                        assertEquals(subcategory.getMaxPossibleScore(), cappedSubcategoryScore, 
                            "Capped subcategory score should equal max possible score");
                    }
                }
            }
        }
        
        // Verify skill transferability total cap was applied if raw score exceeds cap
        if (rawSkillTransferabilityScore > 100) {  // 100 is the cap for skill transferability
            System.out.println("\nSKILL TRANSFERABILITY CATEGORY CAP APPLIED:");
            System.out.println("  Raw score (sum of all qualifying fields): " + rawSkillTransferabilityScore);
            System.out.println("  Capped score (enforced maximum): " + cappedSkillTransferabilityScore);
            
            // The capped score should be exactly 100 if the raw score exceeds 100
            assertEquals(100, cappedSkillTransferabilityScore, 
                "Skill Transferability score should be capped at 100");
        }
        
        System.out.println("\n============== CAP ENFORCEMENT VERIFICATION ==============");
        System.out.println("All categories and subcategories successfully respecting their caps.");
        
        // Verify total is valid and less than or equal to the maximum possible CRS score (1200)
        assertTrue(evaluation.getTotalScore() <= 1200, 
            "Total score should not exceed maximum possible CRS score (1200)");
        
        System.out.println("Total Score: " + evaluation.getTotalScore() + " / 1200");
        System.out.println("============== END CAP ENFORCEMENT TEST ==============");
    }
} 