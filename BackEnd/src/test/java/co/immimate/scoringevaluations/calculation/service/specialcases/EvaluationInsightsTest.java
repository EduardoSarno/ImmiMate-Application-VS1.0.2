package co.immimate.scoringevaluations.calculation.service.specialcases;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import co.immimate.scoringevaluations.calculation.service.EvaluationService;
import co.immimate.scoringevaluations.calculation.service.LogicExpressionEvaluator;
import co.immimate.scoringevaluations.evaluation.model.Evaluation;
import co.immimate.scoringevaluations.evaluation.model.EvaluationCategory;
import co.immimate.scoringevaluations.evaluation.model.EvaluationField;
import co.immimate.scoringevaluations.evaluation.model.EvaluationSubcategory;
import co.immimate.scoringevaluations.evaluation.repository.EvaluationCategoryRepository;
import co.immimate.scoringevaluations.evaluation.repository.EvaluationFieldRepository;
import co.immimate.scoringevaluations.evaluation.repository.EvaluationRepository;
import co.immimate.scoringevaluations.evaluation.repository.EvaluationSubcategoryRepository;
import co.immimate.scoringevaluations.grid.model.Grid;
import co.immimate.scoringevaluations.grid.model.GridCategory;
import co.immimate.scoringevaluations.grid.model.GridField;
import co.immimate.scoringevaluations.grid.model.GridSubcategory;
import co.immimate.scoringevaluations.grid.repository.GridCategoryRepository;
import co.immimate.scoringevaluations.grid.repository.GridFieldRepository;
import co.immimate.scoringevaluations.grid.repository.GridRepository;
import co.immimate.scoringevaluations.grid.repository.GridSubcategoryRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EvaluationInsightsTest {

    @Mock
    private GridRepository gridRepository;
    
    @Mock
    private GridCategoryRepository gridCategoryRepository;
    
    @Mock
    private GridSubcategoryRepository gridSubcategoryRepository;
    
    @Mock
    private GridFieldRepository gridFieldRepository;
    
    @Mock
    private EvaluationRepository evaluationRepository;
    
    @Mock
    private EvaluationCategoryRepository evaluationCategoryRepository;
    
    @Mock
    private EvaluationSubcategoryRepository evaluationSubcategoryRepository;
    
    @Mock
    private EvaluationFieldRepository evaluationFieldRepository;
    
    @Mock
    private LogicExpressionEvaluator logicExpressionEvaluator;
    
    @Mock
    private SkillTransferabilityCappingService skillTransferabilityCappingService;
    
    @InjectMocks
    private EvaluationService evaluationService;
    
    private UUID applicationId;
    private Grid testGrid;
    private List<GridCategory> testCategories;
    private Map<String, Object> userVariables;
    
    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        
        // Make all mocks lenient to address UnnecessaryStubbingException
        Mockito.lenient().when(evaluationFieldRepository.findBySubcatEvalId(any()))
                .thenAnswer(invocation -> {
                    UUID subcatId = invocation.getArgument(0);
                    List<EvaluationField> fields = new ArrayList<>();
                    
                    // Create a qualifying field to return
                    EvaluationField qualifyingField = new EvaluationField();
                    qualifyingField.setFieldEvalId(UUID.randomUUID());
                    qualifyingField.setSubcatEvalId(subcatId);
                    qualifyingField.setFieldName("Test Field");
                    qualifyingField.setUserQualifies(true);
                    qualifyingField.setUserPointsEarned(25);
                    qualifyingField.setApplicationId(applicationId);
                    qualifyingField.setFieldId(UUID.randomUUID());
                    qualifyingField.setCreatedAt(Instant.now());
                    qualifyingField.setUpdatedAt(Instant.now());
                    fields.add(qualifyingField);
                    
                    return fields;
                });
        
        // Setup mock for category repository - using lenient() to avoid unnecessary stubbing exceptions
        Mockito.lenient().when(evaluationCategoryRepository.findById(any(UUID.class)))
                .thenReturn(Optional.of(new EvaluationCategory()));
        
        // Setup mock for subcategory repository
        Mockito.lenient().when(evaluationSubcategoryRepository.findById(any(UUID.class)))
                .thenReturn(Optional.of(new EvaluationSubcategory()));
        
        // Create test data
        applicationId = UUID.randomUUID();
        userVariables = createTestUserVariables();
        
        // Set up grid
        testGrid = new Grid();
        testGrid.setGridId(UUID.randomUUID());
        testGrid.setGridName("Test Express Entry Grid");
        
        // Set up categories
        testCategories = new ArrayList<>();
        testCategories.add(createCategory("Core Human Capital", 460, 500));
        testCategories.add(createCategory("Spouse Factors", 40, 0));
        testCategories.add(createCategory("Skill Transferability", 100, 100));
        
        // Set up mocks
        when(gridRepository.findByGridName(anyString())).thenReturn(Optional.of(testGrid));
        when(gridCategoryRepository.findByGridId(any(UUID.class))).thenReturn(testCategories);
        
        // Set up subcategories for each category
        for (GridCategory category : testCategories) {
            List<GridSubcategory> subcategories = createSubcategoriesForCategory(category);
            when(gridSubcategoryRepository.findByCategoryId(category.getCategoryId())).thenReturn(subcategories);
            
            // Set up fields for each subcategory
            for (GridSubcategory subcategory : subcategories) {
                List<GridField> fields = createFieldsForSubcategory(subcategory);
                when(gridFieldRepository.findBySubcategoryId(subcategory.getSubcategoryId())).thenReturn(fields);
            }
        }
        
        // Set up repository saves
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(i -> i.getArgument(0));
        when(evaluationCategoryRepository.save(any(EvaluationCategory.class))).thenAnswer(i -> i.getArgument(0));
        when(evaluationSubcategoryRepository.save(any(EvaluationSubcategory.class))).thenAnswer(i -> i.getArgument(0));
        when(evaluationFieldRepository.save(any(EvaluationField.class))).thenAnswer(i -> i.getArgument(0));
        
        // Set up logic evaluator
        when(logicExpressionEvaluator.evaluateLogicExpression(anyString(), any(), any())).thenReturn(true);
        
        // Set up skill transferability capping service with capping information
        when(skillTransferabilityCappingService.applySkillTransferabilityGroupCaps(any(), any()))
            .thenAnswer(i -> {
                SkillTransferabilityCappingService.CappingDetails cappingDetails = i.getArgument(1);
                // Make sure details is not null
                if (cappingDetails != null) {
                    // Use the new public API to add capping information
                    cappingDetails.addShortNote("Education group capped from 60 to 50 points.");
                    cappingDetails.addShortNote("Overall Skill Transferability score capped from 120 to 100 points.");
                    cappingDetails.recordGroupScores("Education", 60, 50);
                    cappingDetails.recordGroupScores("Foreign work experience", 60, 50);
                    cappingDetails.recordTotalScores(120, 100);
                }
                return 100; // Return capped score
            });
            
        // Mock findById for EvaluationCategory to ensure proper category mapping
        Mockito.lenient().when(evaluationCategoryRepository.findById(any(UUID.class))).thenAnswer(i -> {
            EvaluationCategory category = new EvaluationCategory();
            category.setCatEvalId((UUID)i.getArgument(0));
            category.setCategoryName("Test Category");
            
            // Set a proper evaluation ID that will match our mocked evaluation
            UUID evalId = UUID.randomUUID();
            category.setEvaluationId(evalId);
            
            // Mock the findById for this evaluation ID
            mockEvaluationForId(evalId);
            
            return Optional.of(category);
        });
        
        // Mock findByCatEvalId for EvaluationSubcategory to ensure proper subcategory mapping
        Mockito.lenient().when(evaluationSubcategoryRepository.findByCatEvalId(any(UUID.class))).thenAnswer(i -> {
            List<EvaluationSubcategory> subcategories = new ArrayList<>();
            EvaluationSubcategory subcategory = new EvaluationSubcategory();
            subcategory.setSubcatEvalId(UUID.randomUUID());
            subcategory.setSubcategoryName("Test Subcategory");
            subcategory.setCatEvalId((UUID)i.getArgument(0));
            subcategories.add(subcategory);
            
            // Mock evaluation fields for this subcategory
            when(evaluationFieldRepository.findBySubcatEvalId(subcategory.getSubcatEvalId()))
                .thenReturn(createEvaluationFieldsForSubcategory(subcategory.getSubcatEvalId()));
            
            return subcategories;
        });
    }
    
    /**
     * Helper method to mock evaluation for a specific ID
     */
    private void mockEvaluationForId(UUID evalId) {
        Evaluation evaluation = new Evaluation();
        evaluation.setEvaluationId(evalId);
        evaluation.setApplicationId(applicationId);
        evaluation.setGridName("Test Express Entry Grid");
        evaluation.setTotalScore(450);
        evaluation.setNotes("Test notes");
        evaluation.setEvaluationDetails("Test evaluation details");
        evaluation.setCreatedAt(Instant.now());
        evaluation.setUpdatedAt(Instant.now());
        evaluation.setStatus("COMPLETED");
        evaluation.setVersion(1);
        
        Mockito.lenient().when(evaluationRepository.findById(evalId)).thenReturn(Optional.of(evaluation));
    }
    
    /**
     * Helper method to create evaluation fields for a subcategory
     */
    private List<EvaluationField> createEvaluationFieldsForSubcategory(UUID subcatEvalId) {
        List<EvaluationField> evalFields = new ArrayList<>();
        
        // Create a qualifying field
        EvaluationField qualifyingField = new EvaluationField();
        qualifyingField.setFieldEvalId(UUID.randomUUID());
        qualifyingField.setSubcatEvalId(subcatEvalId);
        qualifyingField.setFieldName("Test Field");
        qualifyingField.setUserQualifies(true);
        qualifyingField.setUserPointsEarned(25);
        qualifyingField.setApplicationId(applicationId);
        qualifyingField.setFieldId(UUID.randomUUID());
        qualifyingField.setCreatedAt(Instant.now());
        qualifyingField.setUpdatedAt(Instant.now());
        evalFields.add(qualifyingField);
        
        return evalFields;
    }
    
    @Test
    @DisplayName("Test that evaluation insights generate meaningful notes")
    public void testEvaluationInsights() {
        // Simulate a scenario where capping occurs
        Evaluation result = evaluationService.createEvaluation(applicationId, "Test Express Entry Grid", userVariables, true);
        
        // Verify that notes are generated
        assertNotNull(result.getNotes(), "Notes should not be null");
        assertNotNull(result.getEvaluationDetails(), "Evaluation details should not be null");
        
        // Check for key sections in notes
        assertTrue(result.getNotes().contains("KEY QUALIFICATIONS"), "Notes should contain key qualifications section");
        assertTrue(result.getNotes().contains("CAPPING APPLIED"), "Notes should contain capping information");
        assertTrue(result.getNotes().contains("TOP SCORING FACTORS"), "Notes should contain top scoring factors");
        
        // Check for common qualification recognition
        assertTrue(result.getNotes().contains("Master's degree"), "Should recognize master's degree");
        assertTrue(result.getNotes().contains("CLB 10"), "Should recognize high language score");
        
        // Check for specific details in the evaluation_details
        String details = result.getEvaluationDetails();
        assertTrue(details.contains("DETAILED EVALUATION REPORT"), "Should have report header");
        assertTrue(details.contains("APPLICANT PROFILE"), "Should include applicant profile");
        assertTrue(details.contains("CATEGORY BREAKDOWN"), "Should include category breakdown");
        assertTrue(details.contains("CAPPING EVENTS"), "Should include capping events");
        
        // Check that the capping information is properly captured
        assertTrue(details.contains("Skill Transferability"), "Should mention Skill Transferability capping");
        
        // Verify that profile details are captured
        assertTrue(details.contains("Age 35"), "Should include age information");
        assertTrue(details.contains("Masters Degree"), "Should include education information");
        assertTrue(details.contains("Canadian experience"), "Should include Canadian experience");
    }
    
    @Test
    @DisplayName("Test that capping events are properly tracked")
    public void testCappingEvents() {
        // Create a scenario with specific capping
        // Set up the skill transferability capping service to return detailed capping information
        when(skillTransferabilityCappingService.applySkillTransferabilityGroupCaps(any(), any()))
            .thenAnswer(i -> {
                SkillTransferabilityCappingService.CappingDetails cappingDetails = i.getArgument(1);
                // Always create a new CappingDetails if null to avoid NPE
                if (cappingDetails == null) {
                    cappingDetails = new SkillTransferabilityCappingService.CappingDetails();
                }
                
                // Use the new public API to add detailed capping information
                String cappingAnalysis = 
                    """
                    SKILL TRANSFERABILITY CAPPING ANALYSIS:
                    -----------------------------------
                    Group: Education
                    - Raw score: 60 points
                    - Group cap: 50 points
                    - CAPPING APPLIED: Group score reduced from 60 to 50 points
                    """;
                
                cappingDetails.addDetailedNote(cappingAnalysis);
                cappingDetails.addShortNote("Skill Transferability: Education group capped from 60 to 50 points");
                
                // Record a subcategory adjustment
                cappingDetails.recordSubcategoryAdjustment("Education and First Official Language Proficiency");
                cappingDetails.recordGroupScores("Education", 60, 50);
                cappingDetails.recordTotalScores(60, 50);
                
                // Simulate that capping occurred
                return 100;
            });
        
        // Execute evaluation
        Evaluation result = evaluationService.createEvaluation(applicationId, "Test Express Entry Grid", userVariables, true);
        
        // Verify capping notes
        assertTrue(result.getNotes().contains("CAPPING APPLIED"), "Notes should contain capping information");
        assertTrue(result.getEvaluationDetails().contains("CAPPING APPLIED"), "Details should contain capping information");
        
        // Check for detailed capping analysis
        assertTrue(result.getEvaluationDetails().contains("SKILL TRANSFERABILITY CAPPING ANALYSIS"), 
            "Should contain detailed capping analysis");
        assertTrue(result.getEvaluationDetails().contains("Group: Education"), 
            "Should mention the Education group in capping analysis");
        assertTrue(result.getEvaluationDetails().contains("Group score reduced from 60 to 50 points"), 
            "Should provide details of the score reduction");
    }
    
    // Helper methods
    
    private Map<String, Object> createTestUserVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("applicant_age", 35);
        variables.put("applicant_education_level", "masters-degree");
        variables.put("primary_language_test_type", "IELTS");
        variables.put("primary_clb_score", 10);
        variables.put("canadian_work_experience_years", 2);
        variables.put("foreign_work_experience_years", 5);
        variables.put("has_job_offer", true);
        variables.put("is_job_offer_lmia_approved", true);
        variables.put("trades_certification", false);
        
        return variables;
    }
    
    private GridCategory createCategory(String name, int maxPointsWithSpouse, int maxPointsNoSpouse) {
        GridCategory category = new GridCategory();
        category.setCategoryId(UUID.randomUUID());
        category.setGridId(testGrid.getGridId());
        category.setCategoryName(name);
        category.setMaxPointsSpouse(maxPointsWithSpouse);
        category.setMaxPointsNoSpouse(maxPointsNoSpouse);
        category.setCreatedAt(Instant.now());
        category.setUpdatedAt(Instant.now());
        
        return category;
    }
    
    private List<GridSubcategory> createSubcategoriesForCategory(GridCategory category) {
        List<GridSubcategory> subcategories = new ArrayList<>();
        
        if (category.getCategoryName().contains("Core")) {
            subcategories.add(createSubcategory(category, "Age", 110, 100));
            subcategories.add(createSubcategory(category, "Education", 150, 140));
            subcategories.add(createSubcategory(category, "Language", 160, 150));
            subcategories.add(createSubcategory(category, "Work Experience", 80, 70));
        } else if (category.getCategoryName().contains("Spouse")) {
            subcategories.add(createSubcategory(category, "Spouse Education", 10, 0));
            subcategories.add(createSubcategory(category, "Spouse Language", 20, 0));
            subcategories.add(createSubcategory(category, "Spouse Work Experience", 10, 0));
        } else if (category.getCategoryName().contains("Skill Transferability")) {
            subcategories.add(createSubcategory(category, "Education and First Official Language Proficiency", 50, 50));
            subcategories.add(createSubcategory(category, "Education and Canadian Work Experience", 50, 50));
            subcategories.add(createSubcategory(category, "Foreign Work Experience and First Official Language Proficiency", 50, 50));
        }
        
        return subcategories;
    }
    
    private GridSubcategory createSubcategory(GridCategory category, String name, int maxPointsWithSpouse, int maxPointsNoSpouse) {
        GridSubcategory subcategory = new GridSubcategory();
        subcategory.setSubcategoryId(UUID.randomUUID());
        subcategory.setCategoryId(category.getCategoryId());
        subcategory.setSubcategoryName(name);
        subcategory.setMaxPointsSpouse(maxPointsWithSpouse);
        subcategory.setMaxPointsNoSpouse(maxPointsNoSpouse);
        subcategory.setCreatedAt(Instant.now());
        subcategory.setUpdatedAt(Instant.now());
        
        return subcategory;
    }
    
    private List<GridField> createFieldsForSubcategory(GridSubcategory subcategory) {
        List<GridField> fields = new ArrayList<>();
        
        // Create different fields based on subcategory type
        if (subcategory.getSubcategoryName().contains("Age")) {
            fields.add(createField(subcategory, "Age 18-35", "applicant_age >= 18 AND applicant_age <= 35", 12, 12));
            fields.add(createField(subcategory, "Age 36-40", "applicant_age >= 36 AND applicant_age <= 40", 6, 6));
        } else if (subcategory.getSubcategoryName().contains("Education")) {
            fields.add(createField(subcategory, "PhD", "applicant_education_level = 'doctoral-degree'", 25, 25));
            fields.add(createField(subcategory, "Master's", "applicant_education_level = 'masters-degree'", 23, 23));
        } else if (subcategory.getSubcategoryName().contains("Language")) {
            fields.add(createField(subcategory, "CLB 9-10", "primary_clb_score >= 9 AND primary_clb_score <= 10", 20, 20));
            fields.add(createField(subcategory, "CLB 7-8", "primary_clb_score >= 7 AND primary_clb_score <= 8", 16, 16));
        } else if (subcategory.getSubcategoryName().contains("Work Experience")) {
            fields.add(createField(subcategory, "1-2 years", "canadian_work_experience_years >= 1 AND canadian_work_experience_years <= 2", 9, 9));
            fields.add(createField(subcategory, "3-4 years", "canadian_work_experience_years >= 3 AND canadian_work_experience_years <= 4", 11, 11));
        } else if (subcategory.getSubcategoryName().contains("First Official Language")) {
            fields.add(createField(subcategory, "CLB 9+ with good education", "primary_clb_score >= 9 AND applicant_education_level IN ('masters-degree', 'doctoral-degree')", 50, 50));
        }
        
        return fields;
    }
    
    private GridField createField(GridSubcategory subcategory, String name, String logicExpression, int pointsWithSpouse, int pointsWithoutSpouse) {
        GridField field = new GridField();
        field.setFieldId(UUID.randomUUID());
        field.setSubcategoryId(subcategory.getSubcategoryId());
        field.setFieldName(name);
        field.setLogicExpression(logicExpression);
        field.setPointsWithSpouse(pointsWithSpouse);
        field.setPointsWithoutSpouse(pointsWithoutSpouse);
        field.setCreatedAt(Instant.now());
        field.setUpdatedAt(Instant.now());
        
        return field;
    }
} 