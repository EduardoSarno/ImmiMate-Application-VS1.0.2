package co.immimate.scoringevaluations.calculation.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.immimate.profile.model.UserImmigrationProfile;
import co.immimate.profile.repository.UserImmigrationProfileRepository;
import co.immimate.scoringevaluations.evaluation.model.Evaluation;
import co.immimate.scoringevaluations.evaluation.model.EvaluationCategory;
import co.immimate.scoringevaluations.evaluation.model.EvaluationField;
import co.immimate.scoringevaluations.evaluation.model.EvaluationSubcategory;
import co.immimate.scoringevaluations.evaluation.repository.EvaluationCategoryRepository;
import co.immimate.scoringevaluations.evaluation.repository.EvaluationFieldRepository;
import co.immimate.scoringevaluations.evaluation.repository.EvaluationRepository;
import co.immimate.scoringevaluations.evaluation.repository.EvaluationSubcategoryRepository;
import co.immimate.scoringevaluations.grid.model.Grid;
import co.immimate.scoringevaluations.grid.repository.GridRepository;

/**
 * This test class persists evaluation data to the database for inspection and debugging.
 * IMPORTANT: This class does NOT use @Transactional to ensure data is committed to the database!
 */
@Component
@SpringBootTest
@ActiveProfiles("test")
public class EvaluationServiceDatabaseTest {

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
    private UserImmigrationProfileRepository profileRepository;
    
    // Use the real application ID from your test database
    private static final UUID TEST_APPLICATION_ID = UUID.fromString("7c75c62d-4d57-43e9-bfd5-a497c86c2c4d");
    private static final String TEST_GRID_NAME = "Comprehensive Ranking System (CRS)";

    @Test
    @DisplayName("Persist evaluation data to database without transaction rollback")
    public void persistEvaluationToDatabase() {
        // First, verify the grid exists
        System.out.println("============== DATABASE PERSISTENCE TEST ==============");
        Grid grid = gridRepository.findByGridName(TEST_GRID_NAME).orElse(null);
        assertNotNull(grid, "Grid should exist: " + TEST_GRID_NAME);
        System.out.println("Found grid: " + grid.getGridName() + " with ID: " + grid.getGridId());
        
        // Generate a unique ID suffix for identifying this test run in the database
        String testRunId = UUID.randomUUID().toString().substring(0, 8);
        System.out.println("Test Run ID: " + testRunId);
        
        // Get user variables and spouse status
        Map<String, Object> userVariables = evaluationService.getUserVariables(TEST_APPLICATION_ID);
        boolean hasSpouse = evaluationService.hasSpouse(TEST_APPLICATION_ID);
        
        // Execute the evaluation with persistence
        Evaluation evaluation = evaluationService.createEvaluation(
            TEST_APPLICATION_ID, 
            TEST_GRID_NAME, 
            userVariables, 
            hasSpouse
        );
        
        // Add a note to identify this evaluation in the database
        evaluation.setNotes("DB Test run " + testRunId + " - For database inspection");
        evaluationRepository.save(evaluation);
        
        // 1. Verify and log the main evaluation record
        System.out.println("============== SAVED EVALUATION DATA ==============");
        System.out.println("Evaluation ID: " + evaluation.getEvaluationId());
        System.out.println("Application ID: " + evaluation.getApplicationId());
        System.out.println("Grid Name: " + evaluation.getGridName());
        System.out.println("Total Score: " + evaluation.getTotalScore());
        
        // 2. Verify and log evaluation categories
        List<EvaluationCategory> categories = categoryRepository.findByEvaluationId(evaluation.getEvaluationId());
        System.out.println("\n============== SAVED CATEGORIES ==============");
        System.out.println("Total Categories: " + categories.size());
        int categoryTotal = 0;
        for (EvaluationCategory category : categories) {
            categoryTotal += category.getUserScore();
            System.out.println("Category: " + category.getCategoryName() + 
                             " (ID: " + category.getCatEvalId() + ", CategoryID: " + category.getCategoryId() + ")");
            System.out.println("  Score: " + category.getUserScore() + " points");
        }
        System.out.println("Total Category Score: " + categoryTotal);
        
        // 3. Verify and log subcategories and fields for a detailed breakdown
        System.out.println("\n============== DETAILED SCORE BREAKDOWN ==============");
        for (EvaluationCategory category : categories) {
            System.out.println("\nCATEGORY: " + category.getCategoryName() + " - " + category.getUserScore() + " points");
            
            List<EvaluationSubcategory> subcategories = subcategoryRepository.findByCatEvalId(category.getCatEvalId());
            for (EvaluationSubcategory subcategory : subcategories) {
                System.out.println("  SUBCATEGORY: " + subcategory.getSubcategoryName() + 
                                 " (ID: " + subcategory.getSubcatEvalId() + ", SubcatID: " + 
                                 subcategory.getSubcategoryId() + ") - " + subcategory.getUserScore() + " points");
                
                List<EvaluationField> fields = fieldRepository.findBySubcatEvalId(subcategory.getSubcatEvalId());
                int fieldsWithPoints = 0;
                for (EvaluationField field : fields) {
                    if (field.getUserPointsEarned() > 0) {
                        fieldsWithPoints++;
                        System.out.println("    FIELD: " + field.getFieldName() + " - " + field.getUserPointsEarned() + " points");
                        System.out.println("      Expression: " + field.getLogicExpression());
                        System.out.println("      Actual Value: " + field.getActualValue());
                        System.out.println("      Qualifies: " + field.getUserQualifies());
                    }
                }
                
                if (fieldsWithPoints == 0) {
                    System.out.println("    (No qualifying fields)");
                }
            }
        }
        
        // Final verification that data was properly persisted
        assertEquals(evaluation.getTotalScore(), categoryTotal, 
                "Total score should match sum of category scores");
        
        System.out.println("\n============== DATABASE INSPECTION INFO ==============");
        System.out.println("DATA HAS BEEN PERSISTED to the database with the following IDs:");
        System.out.println("Evaluation ID: " + evaluation.getEvaluationId());
        System.out.println("Application ID: " + evaluation.getApplicationId());
        System.out.println("Test Run ID: " + testRunId);
        System.out.println("\nYou can query the database directly with:");
        System.out.println("SELECT * FROM evaluations WHERE evaluation_id = '" + 
                         evaluation.getEvaluationId() + "';");
        System.out.println("\nTo view all categories:");
        System.out.println("SELECT * FROM evaluation_categories WHERE evaluation_id = '" + 
                         evaluation.getEvaluationId() + "';");
        System.out.println("=============================================================");
    }
    
    @Test
    @DisplayName("Test evaluation creation similar to controller workflow")
    public void testEvaluationControllerFlow() {
        // First, verify the grid exists
        System.out.println("============== CONTROLLER FLOW TEST ==============");
        Grid grid = gridRepository.findByGridName(TEST_GRID_NAME).orElse(null);
        assertNotNull(grid, "Grid should exist: " + TEST_GRID_NAME);
        
        // Simulate what happens in the controller
        System.out.println("Simulating controller workflow for application ID: " + TEST_APPLICATION_ID);
        
        // 1. Controller gets user variables from service
        Map<String, Object> userVariables = evaluationService.getUserVariables(TEST_APPLICATION_ID);
        System.out.println("Retrieved " + userVariables.size() + " user variables");
        
        // 2. Controller checks if user has spouse
        boolean hasSpouse = evaluationService.hasSpouse(TEST_APPLICATION_ID);
        System.out.println("User has spouse: " + hasSpouse);
        
        // 3. Controller creates evaluation
        Evaluation evaluation = evaluationService.createEvaluation(
            TEST_APPLICATION_ID, 
            TEST_GRID_NAME, 
            userVariables, 
            hasSpouse
        );
        
        // 4. Controller adds a description and saves
        evaluation.setNotes("Created via controller flow test");
        evaluationRepository.save(evaluation);
        
        // 5. Assert results
        assertNotNull(evaluation.getEvaluationId(), "Evaluation ID should not be null");
        
        System.out.println("Created evaluation with ID: " + evaluation.getEvaluationId());
        System.out.println("Total score: " + evaluation.getTotalScore());
        
        // Get categories to verify results
        List<EvaluationCategory> categories = categoryRepository.findByEvaluationId(evaluation.getEvaluationId());
        int categoryTotal = categories.stream().mapToInt(EvaluationCategory::getUserScore).sum();
        
        assertEquals(evaluation.getTotalScore(), categoryTotal, 
                "Total score should match sum of category scores");
        
        System.out.println("All " + categories.size() + " categories processed successfully");
        System.out.println("============== END CONTROLLER FLOW TEST ==============");
    }

    @Test
    @DisplayName("Test category and subcategory caps are correctly applied")
    public void testScoreCaps() {
        System.out.println("============== CATEGORY AND SUBCATEGORY CAPS TEST ==============");
        Grid grid = gridRepository.findByGridName(TEST_GRID_NAME).orElse(null);
        assertNotNull(grid, "Grid should exist: " + TEST_GRID_NAME);
        
        // Get user variables and spouse status
        Map<String, Object> userVariables = evaluationService.getUserVariables(TEST_APPLICATION_ID);
        boolean hasSpouse = evaluationService.hasSpouse(TEST_APPLICATION_ID);
        
        // Execute the evaluation
        Evaluation evaluation = evaluationService.createEvaluation(
            TEST_APPLICATION_ID, 
            TEST_GRID_NAME, 
            userVariables, 
            hasSpouse
        );
        
        evaluation.setNotes("Cap verification test");
        evaluationRepository.save(evaluation);
        
        System.out.println("Created evaluation with ID: " + evaluation.getEvaluationId());
        System.out.println("Total score: " + evaluation.getTotalScore());
        
        // Get all categories to verify caps
        List<EvaluationCategory> categories = categoryRepository.findByEvaluationId(evaluation.getEvaluationId());
        
        System.out.println("\n============== VERIFYING CATEGORY CAPS ==============");
        int categoriesWithinCaps = 0;
        boolean skillTransferabilityFound = false;
        
        for (EvaluationCategory category : categories) {
            // Verify category score doesn't exceed max possible score
            assertTrue(category.getUserScore() <= category.getMaxPossibleScore(), 
                "Category '" + category.getCategoryName() + "' score (" + category.getUserScore() + 
                ") exceeds max possible score (" + category.getMaxPossibleScore() + ")");
            
            if (category.getUserScore() <= category.getMaxPossibleScore()) {
                categoriesWithinCaps++;
            }
            
            System.out.println("Category: " + category.getCategoryName() + 
                " - Score: " + category.getUserScore() + 
                " / Max: " + category.getMaxPossibleScore());
            
            // Check if this is the Skill Transferability category
            if (category.getCategoryName().contains("Skill Transferability")) {
                skillTransferabilityFound = true;
            }
            
            // Get all subcategories to verify their caps
            List<EvaluationSubcategory> subcategories = subcategoryRepository.findByCatEvalId(category.getCatEvalId());
            int subcategoriesWithinCaps = 0;
            
            for (EvaluationSubcategory subcategory : subcategories) {
                // Verify subcategory score doesn't exceed max possible score
                assertTrue(subcategory.getUserScore() <= subcategory.getMaxPossibleScore(), 
                    "Subcategory '" + subcategory.getSubcategoryName() + "' score (" + subcategory.getUserScore() + 
                    ") exceeds max possible score (" + subcategory.getMaxPossibleScore() + ")");
                
                if (subcategory.getUserScore() <= subcategory.getMaxPossibleScore()) {
                    subcategoriesWithinCaps++;
                }
                
                System.out.println("  Subcategory: " + subcategory.getSubcategoryName() + 
                    " - Score: " + subcategory.getUserScore() + 
                    " / Max: " + subcategory.getMaxPossibleScore());
                
                // If in Skill Transferability, also verify the individual fields to check if the sum would naturally exceed the cap
                if (category.getCategoryName().contains("Skill Transferability")) {
                    List<EvaluationField> fields = fieldRepository.findBySubcatEvalId(subcategory.getSubcatEvalId());
                    
                    // Calculate the raw score (sum of all field points, without caps)
                    // This helps us verify if capping is actually needed and applied
                    int rawFieldScore = 0;
                    for (EvaluationField field : fields) {
                        if (field.getUserQualifies()) {
                            rawFieldScore += field.getUserPointsEarned();
                            System.out.println("    Field: " + field.getFieldName() + 
                                " - Points: " + field.getUserPointsEarned() +
                                " - Value: " + field.getActualValue());
                        }
                    }
                    
                    // If raw score exceeds the max, but subcategory score is within limit, capping worked
                    if (rawFieldScore > subcategory.getMaxPossibleScore() && 
                        subcategory.getUserScore() <= subcategory.getMaxPossibleScore()) {
                        System.out.println("    Cap successfully applied! Raw score: " + rawFieldScore + 
                            " capped to: " + subcategory.getUserScore());
                    }
                }
            }
            
            // Verify all subcategories are within caps
            assertEquals(subcategories.size(), subcategoriesWithinCaps, 
                "All subcategories should be within their maximum possible score");
        }
        
        // Verify all categories are within caps
        assertEquals(categories.size(), categoriesWithinCaps, 
            "All categories should be within their maximum possible score");
        
        // Ensure we found and tested the Skill Transferability category
        assertTrue(skillTransferabilityFound, "Skill Transferability category should be present");
        
        System.out.println("All " + categories.size() + " categories and their subcategories are within caps");
        System.out.println("============== END CATEGORY AND SUBCATEGORY CAPS TEST ==============");
    }

    @Test
    @DisplayName("Test with JSON profile data persisted to the database")
    public void testWithJsonProfileData() {
        System.out.println("============== JSON PROFILE DATA TEST ==============");
        
        // First, fetch the user profile
        UserImmigrationProfile profile = profileRepository.findByApplicationId(TEST_APPLICATION_ID)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for test application"));
        
        // Create a detailed JSON representation of the profile
        String jsonProfile = createJsonProfileData(profile);
        
        // Update the profile with the JSON data
        profile.setJsonPayload(jsonProfile);
        profileRepository.save(profile);
        
        System.out.println("Updated profile with JSON data: " + profile.getApplicationId());
        System.out.println("JSON payload (sample): " + jsonProfile.substring(0, Math.min(100, jsonProfile.length())) + "...");
        
        // Run a standard evaluation to see if the JSON data is included
        Map<String, Object> userVariables = evaluationService.getUserVariables(TEST_APPLICATION_ID);
        boolean hasSpouse = evaluationService.hasSpouse(TEST_APPLICATION_ID);
        
        // Execute the evaluation
        Evaluation evaluation = evaluationService.createEvaluation(
            TEST_APPLICATION_ID, 
            TEST_GRID_NAME, 
            userVariables, 
            hasSpouse
        );
        
        evaluation.setNotes("Evaluation with JSON profile data");
        evaluationRepository.save(evaluation);
        
        System.out.println("Created evaluation with ID: " + evaluation.getEvaluationId());
        System.out.println("Total score: " + evaluation.getTotalScore());
        
        // Print some user variables to verify JSON data was included
        System.out.println("\n============== USER VARIABLES GENERATED ==============");
        System.out.println("Total Variables: " + userVariables.size());
        
        // Print a subset of key variables
        String[] keyVars = {"applicant_age", "applicant_education_level", "primary_clb_score", 
                            "canadian_work_experience_years", "trades_certification", "json_payload"};
        
        for (String key : keyVars) {
            System.out.println(key + " = " + userVariables.get(key));
        }
        
        System.out.println("============== END JSON PROFILE DATA TEST ==============");
    }
    
    /**
     * Creates a detailed JSON representation of the profile for testing.
     * This demonstrates how JSON data can be included alongside the structured fields.
     */
    private String createJsonProfileData(UserImmigrationProfile profile) {
        try {
            // Create a map with additional profile information not captured in the regular fields
            Map<String, Object> jsonData = new HashMap<>();
            
            // Add basic profile information
            Map<String, Object> basicInfo = new HashMap<>();
            basicInfo.put("fullName", profile.getApplicantName());
            basicInfo.put("age", profile.getApplicantAge());
            basicInfo.put("citizenship", profile.getApplicantCitizenship());
            basicInfo.put("currentResidence", profile.getApplicantResidence());
            basicInfo.put("maritalStatus", profile.getApplicantMaritalStatus());
            basicInfo.put("email", profile.getUserEmail());
            jsonData.put("basicInfo", basicInfo);
            
            // Add education details with additional context
            Map<String, Object> education = new HashMap<>();
            education.put("highestLevel", profile.getApplicantEducationLevel());
            education.put("completedInCanada", profile.getEducationCompletedInCanada());
            education.put("canadianEducationLevel", profile.getCanadianEducationLevel());
            education.put("hasECA", profile.isHasEducationalCredentialAssessment());
            education.put("graduationYear", 2019); // Example of additional data
            education.put("fieldOfStudy", "Computer Science"); // Example of additional data
            education.put("institution", "University of Toronto"); // Example of additional data
            jsonData.put("education", education);
            
            // Add language test details
            Map<String, Object> languageTests = new HashMap<>();
            
            Map<String, Object> primaryTest = new HashMap<>();
            primaryTest.put("type", profile.getPrimaryLanguageTestType());
            primaryTest.put("speaking", profile.getPrimaryTestSpeakingScore());
            primaryTest.put("listening", profile.getPrimaryTestListeningScore());
            primaryTest.put("reading", profile.getPrimaryTestReadingScore());
            primaryTest.put("writing", profile.getPrimaryTestWritingScore());
            primaryTest.put("testDate", "2023-06-15"); // Example of additional data
            primaryTest.put("expiryDate", "2025-06-15"); // Example of additional data
            languageTests.put("primary", primaryTest);
            
            if (profile.isTookSecondaryLanguageTest()) {
                Map<String, Object> secondaryTest = new HashMap<>();
                secondaryTest.put("type", profile.getSecondaryTestType());
                secondaryTest.put("speaking", profile.getSecondaryTestSpeakingScore());
                secondaryTest.put("listening", profile.getSecondaryTestListeningScore());
                secondaryTest.put("reading", profile.getSecondaryTestReadingScore());
                secondaryTest.put("writing", profile.getSecondaryTestWritingScore());
                secondaryTest.put("testDate", "2023-07-20"); // Example of additional data
                secondaryTest.put("expiryDate", "2025-07-20"); // Example of additional data
                languageTests.put("secondary", secondaryTest);
            }
            
            jsonData.put("languageTests", languageTests);
            
            // Add work experience details
            Map<String, Object> workExperience = new HashMap<>();
            workExperience.put("canadianYears", profile.getCanadianWorkExperienceYears());
            workExperience.put("foreignYears", profile.getForeignWorkExperienceYears());
            workExperience.put("currentlyWorking", profile.isWorkingInCanada());
            workExperience.put("canadianNocCode", profile.getNocCodeCanadian());
            workExperience.put("foreignNocCode", profile.getNocCodeForeign());
            
            // Add job history as additional information
            List<Map<String, Object>> jobHistory = new ArrayList<>();
            
            // Example Canadian job
            Map<String, Object> canadianJob = new HashMap<>();
            canadianJob.put("employer", "Canadian Tech Inc.");
            canadianJob.put("position", "Software Developer");
            canadianJob.put("location", "Toronto, Canada");
            canadianJob.put("startDate", "2020-01-15");
            canadianJob.put("endDate", "2023-01-15");
            canadianJob.put("nocCode", profile.getNocCodeCanadian());
            canadianJob.put("fullTime", true);
            canadianJob.put("hoursPerWeek", 40);
            jobHistory.add(canadianJob);
            
            // Example foreign job
            Map<String, Object> foreignJob = new HashMap<>();
            foreignJob.put("employer", "International Systems Ltd.");
            foreignJob.put("position", "Junior Developer");
            foreignJob.put("location", "London, UK");
            foreignJob.put("startDate", "2018-03-10");
            foreignJob.put("endDate", "2019-12-20");
            foreignJob.put("nocCode", profile.getNocCodeForeign());
            foreignJob.put("fullTime", true);
            foreignJob.put("hoursPerWeek", 35);
            jobHistory.add(foreignJob);
            
            workExperience.put("jobHistory", jobHistory);
            jsonData.put("workExperience", workExperience);
            
            // Add additional information about trade certification
            Map<String, Object> tradesCertification = new HashMap<>();
            tradesCertification.put("hasCertification", profile.getTradesCertification());
            tradesCertification.put("certificationType", "Red Seal"); // Example data
            tradesCertification.put("issueDate", "2021-11-05"); // Example data
            tradesCertification.put("certificationNumber", "RS12345678"); // Example data
            jsonData.put("tradesCertification", tradesCertification);
            
            // Other profile sections can be added similarly
            
            // Convert the map to a JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(jsonData);
            
        } catch (JsonProcessingException e) {
            System.err.println("Error creating JSON profile data: " + e.getMessage());
            return "{}"; // Return empty JSON object on error
        }
    }
} 