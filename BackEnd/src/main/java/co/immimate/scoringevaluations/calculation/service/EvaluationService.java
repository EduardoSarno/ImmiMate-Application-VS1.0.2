package co.immimate.scoringevaluations.calculation.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.immimate.profile.model.UserImmigrationProfile;
import co.immimate.profile.repository.UserImmigrationProfileRepository;
import co.immimate.scoringevaluations.calculation.service.specialcases.SkillTransferabilityCappingService;
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
import co.immimate.user.model.User;

/**
 * Service for evaluating immigration profiles against scoring grids.
 * Implements a flexible and dynamic scoring evaluation system.
 */
@Service
public class EvaluationService {

    private static final Logger logger = LoggerFactory.getLogger(EvaluationService.class);
    
    // Constants for evaluation status
    private static final String STATUS_COMPLETED = "COMPLETED";
    
    // Constants for initial version number
    private static final int INITIAL_VERSION = 1;
    
    // Constants for getter method prefixes
    private static final String GET_PREFIX = "get";
    private static final String IS_PREFIX = "is";
    private static final int GET_PREFIX_LENGTH = 3;
    private static final int IS_PREFIX_LENGTH = 2;
    
    // Constants for marital status
    private static final String MARITAL_STATUS_MARRIED = "MARRIED";
    private static final String MARITAL_STATUS_COMMON_LAW = "COMMON_LAW";

    // Constants for regular expressions
    private static final String EXPRESSION_SEPARATOR = ";";
    private static final String OR_OPERATOR = " OR ";
    
    @Autowired
    private GridRepository gridRepository;
    
    @Autowired
    private GridCategoryRepository gridCategoryRepository;
    
    @Autowired
    private GridSubcategoryRepository gridSubcategoryRepository;
    
    @Autowired
    private GridFieldRepository gridFieldRepository;
    
    @Autowired
    private EvaluationRepository evaluationRepository;
    
    @Autowired
    private EvaluationCategoryRepository evaluationCategoryRepository;
    
    @Autowired
    private EvaluationSubcategoryRepository evaluationSubcategoryRepository;
    
    @Autowired
    private EvaluationFieldRepository evaluationFieldRepository;
    
    @Autowired
    private LogicExpressionEvaluator logicExpressionEvaluator;
    
    @Autowired
    private UserImmigrationProfileRepository profileRepository;
    
    @Autowired
    private SkillTransferabilityCappingService skillTransferabilityCappingService;
    
    /**
     * Class to track notable events and details during evaluation for user-facing documentation.
     */
    private static class EvaluationInsights {
        private final StringBuilder shortNotes = new StringBuilder();
        private final StringBuilder detailedNotes = new StringBuilder();
        private final Map<String, List<String>> categoryHighlights = new HashMap<>();
        private final List<String> keyQualifications = new ArrayList<>();
        private final List<String> cappingEvents = new ArrayList<>();
        private final List<String> significantFactors = new ArrayList<>();
        
        public void addCategoryHighlight(String categoryName, String highlight) {
            categoryHighlights.computeIfAbsent(categoryName, k -> new ArrayList<>()).add(highlight);
            
            // Add significant score contributions to the significant factors
            if (highlight.contains("contributing") && highlight.contains("points")) {
                significantFactors.add(String.format("[%s] %s", categoryName, highlight));
            }
        }
        
        public void addQualification(String qualification) {
            keyQualifications.add(qualification);
        }
        
        public void addCappingEvent(String cappingEvent) {
            cappingEvents.add(cappingEvent);
            
            // Also add a brief note to the short notes
            if (shortNotes.length() > 0) {
                shortNotes.append("\n");
            }
            shortNotes.append("[CAPPING] ").append(cappingEvent);
        }
        
        public void addSignificantFactor(String factor) {
            significantFactors.add(factor);
        }
        
        /**
         * Compiles all insights into a concise summary for the notes field
         */
        public String generateSummaryNotes() {
            StringBuilder summary = new StringBuilder();
            
            // Add key qualifications
            if (!keyQualifications.isEmpty()) {
                summary.append("KEY QUALIFICATIONS:\n");
                for (String qualification : keyQualifications) {
                    summary.append("- ").append(qualification).append("\n");
                }
                summary.append("\n");
            }
            
            // Add capping events
            if (!cappingEvents.isEmpty()) {
                summary.append("CAPPING APPLIED:\n");
                for (String event : cappingEvents) {
                    summary.append("- ").append(event).append("\n");
                }
                summary.append("\n");
            }
            
            // Add top 3 significant factors
            if (!significantFactors.isEmpty()) {
                summary.append("TOP SCORING FACTORS:\n");
                int count = 0;
                for (String factor : significantFactors) {
                    summary.append("- ").append(factor).append("\n");
                    count++;
                    if (count >= 3) break;
                }
            }
            
            // Add the custom short notes
            if (shortNotes.length() > 0) {
                summary.append("\nADDITIONAL NOTES:\n").append(shortNotes);
            }
            
            return summary.toString();
        }
        
        /**
         * Compiles all insights into a comprehensive report for the evaluation_details field
         */
        public String generateDetailedReport() {
            StringBuilder report = new StringBuilder();
            
            report.append("=======================================\n");
            report.append("DETAILED EVALUATION REPORT\n");
            report.append("=======================================\n\n");
            
            // Generate timestamp
            report.append("Generated: ").append(Instant.now()).append("\n\n");
            
            // Add category-by-category breakdown
            report.append("CATEGORY BREAKDOWN:\n");
            report.append("-------------------\n");
            for (Map.Entry<String, List<String>> entry : categoryHighlights.entrySet()) {
                report.append(entry.getKey()).append(":\n");
                for (String highlight : entry.getValue()) {
                    report.append("- ").append(highlight).append("\n");
                }
                report.append("\n");
            }
            
            // Add key qualifications section
            if (!keyQualifications.isEmpty()) {
                report.append("KEY QUALIFICATIONS:\n");
                report.append("------------------\n");
                for (String qualification : keyQualifications) {
                    report.append("- ").append(qualification).append("\n");
                }
                report.append("\n");
            }
            
            // Add capping events section
            if (!cappingEvents.isEmpty()) {
                report.append("CAPPING EVENTS:\n");
                report.append("--------------\n");
                for (String event : cappingEvents) {
                    report.append("- ").append(event).append("\n");
                }
                report.append("\n");
            }
            
            // Add significant factors section
            if (!significantFactors.isEmpty()) {
                report.append("SIGNIFICANT FACTORS:\n");
                report.append("-------------------\n");
                for (String factor : significantFactors) {
                    report.append("- ").append(factor).append("\n");
                }
                report.append("\n");
            }
            
            // Add the detailed notes
            if (detailedNotes.length() > 0) {
                report.append("DETAILED TECHNICAL NOTES:\n");
                report.append("------------------------\n");
                report.append(detailedNotes);
            }
            
            return report.toString();
        }
    }
    
    /**
     * Creates a new evaluation for the given application using the specified grid.
     * 
     * @param applicationId The ID of the application to evaluate
     * @param gridName The name of the grid to use for evaluation
     * @param userVariables Map of user variables to use for evaluating logic expressions
     * @param hasSpouse Whether the applicant has a spouse
     * @return The created evaluation
     */
    @Transactional
    public Evaluation createEvaluation(UUID applicationId, String gridName, 
                                     Map<String, Object> userVariables, boolean hasSpouse) {
        logger.info("Creating evaluation for application {} using grid {}", applicationId, gridName);
        
        // Create an insights tracker to collect notable information during evaluation
        EvaluationInsights insights = new EvaluationInsights();
        
        // Find the specified grid
        Grid grid = gridRepository.findByGridName(gridName)
                .orElseThrow(() -> new IllegalArgumentException("Grid not found: " + gridName));
        
        // Record key profile details in the insights
        recordProfileInsights(userVariables, hasSpouse, insights);
        
        // Create the evaluation
        Evaluation evaluation = new Evaluation();
        evaluation.setEvaluationId(UUID.randomUUID());
        evaluation.setApplicationId(applicationId);
        evaluation.setGridId(grid.getGridId());
        evaluation.setGridName(grid.getGridName());
        evaluation.setEvaluationDate(Instant.now());
        evaluation.setTotalScore(0);  // Will be updated after all calculations
        evaluation.setCreatedAt(Instant.now());
        evaluation.setUpdatedAt(Instant.now());
        evaluation.setStatus(STATUS_COMPLETED);
        evaluation.setVersion(INITIAL_VERSION);
        
        // Save the evaluation
        evaluation = evaluationRepository.save(evaluation);
        
        // Process all categories in the grid
        List<GridCategory> categories = gridCategoryRepository.findByGridId(grid.getGridId());
        int totalScore = 0;
        
        for (GridCategory category : categories) {
            EvaluationCategory evalCategory = processCategory(evaluation, category, userVariables, hasSpouse, insights);
            totalScore += evalCategory.getUserScore();
            
            // Record category score in insights
            insights.addCategoryHighlight(category.getCategoryName(), 
                String.format("Scored %d out of possible %d points", 
                evalCategory.getUserScore(), evalCategory.getMaxPossibleScore()));
            
            // Add significant category contributions to insights
            if (evalCategory.getUserScore() > 0) {
                insights.addSignificantFactor(String.format("%s contributed %d points", 
                    category.getCategoryName(), evalCategory.getUserScore()));
            }
        }
        
        // Update total score
        evaluation.setTotalScore(totalScore);
        
        // Update the evaluation with collected insights
        evaluation.setNotes(insights.generateSummaryNotes());
        evaluation.setEvaluationDetails(insights.generateDetailedReport());
        
        evaluation = evaluationRepository.save(evaluation);
        
        logger.info("Evaluation created with ID {} and total score {}", evaluation.getEvaluationId(), totalScore);
        return evaluation;
    }
    
    /**
     * Records key profile information as insights for the evaluation report.
     */
    private void recordProfileInsights(Map<String, Object> userVariables, 
                                      boolean hasSpouse, EvaluationInsights insights) {
        // Add profile summary
        StringBuilder profileSummary = new StringBuilder();
        
        // Extract key variables
        Object age = userVariables.get("applicant_age");
        Object education = userVariables.get("applicant_education_level");
        Object primaryLanguage = userVariables.get("primary_language_test_type");
        Object primaryCLB = userVariables.get("primary_clb_score");
        Object canadianExp = userVariables.get("canadian_work_experience_years");
        Object foreignExp = userVariables.get("foreign_work_experience_years");
        
        profileSummary.append("Profile Overview: ");
        
        if (age != null) {
            profileSummary.append("Age ").append(age).append(", ");
        }
        
        if (education != null) {
            String educationStr = education.toString().replace("-", " ");
            profileSummary.append(capitalizeString(educationStr)).append(", ");
        }
        
        if (primaryLanguage != null && primaryCLB != null) {
            profileSummary.append("Language: CLB ").append(primaryCLB).append(", ");
        }
        
        if (canadianExp != null) {
            int yearsCanadian = Integer.parseInt(canadianExp.toString());
            if (yearsCanadian > 0) {
                profileSummary.append(yearsCanadian).append(" year").append(yearsCanadian != 1 ? "s" : "")
                    .append(" Canadian experience, ");
            }
        }
        
        if (foreignExp != null) {
            int yearsForeign = Integer.parseInt(foreignExp.toString());
            if (yearsForeign > 0) {
                profileSummary.append(yearsForeign).append(" year").append(yearsForeign != 1 ? "s" : "")
                    .append(" foreign experience, ");
            }
        }
        
        profileSummary.append(hasSpouse ? "With spouse" : "Without spouse");
        
        // Add profile summary to detailed report
        insights.detailedNotes.append("APPLICANT PROFILE:\n");
        insights.detailedNotes.append("-----------------\n");
        insights.detailedNotes.append(profileSummary).append("\n\n");
        
        // Add key qualifications
        addKeyQualifications(userVariables, insights);
    }
    
    /**
     * Extracts and adds key qualifications from user variables to insights.
     */
    private void addKeyQualifications(Map<String, Object> userVariables, EvaluationInsights insights) {
        // Check for advanced degrees
        Object education = userVariables.get("applicant_education_level");
        if (education != null) {
            String educationStr = education.toString().toLowerCase();
            if (educationStr.contains("doctoral") || educationStr.contains("phd")) {
                insights.addQualification("Doctoral degree (highest education)");
            } else if (educationStr.contains("master")) {
                insights.addQualification("Master's degree");
            }
        }
        
        // Check for high language proficiency
        Object primaryCLB = userVariables.get("primary_clb_score");
        if (primaryCLB != null) {
            int clbScore = Integer.parseInt(primaryCLB.toString());
            if (clbScore >= 9) {
                insights.addQualification("High language proficiency (CLB " + clbScore + ")");
            }
        }
        
        // Check for significant Canadian experience
        Object canadianExp = userVariables.get("canadian_work_experience_years");
        if (canadianExp != null) {
            int years = Integer.parseInt(canadianExp.toString());
            if (years >= 3) {
                insights.addQualification("Significant Canadian work experience (" + years + " years)");
            }
        }
        
        // Check for provincial nomination
        Object provincialNomination = userVariables.get("has_provincial_nomination");
        if (provincialNomination != null && Boolean.parseBoolean(provincialNomination.toString())) {
            insights.addQualification("Provincial nomination");
        }
        
        // Check for job offer
        Object jobOffer = userVariables.get("has_job_offer");
        if (jobOffer != null && Boolean.parseBoolean(jobOffer.toString())) {
            Object lmiaApproved = userVariables.get("is_job_offer_lmia_approved");
            if (lmiaApproved != null && Boolean.parseBoolean(lmiaApproved.toString())) {
                insights.addQualification("LMIA-approved job offer");
            } else {
                insights.addQualification("Job offer");
            }
        }
    }
    
    /**
     * Capitalizes the first letter of each word in a string.
     */
    private String capitalizeString(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        StringBuilder result = new StringBuilder();
        String[] words = str.split("\\s");
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        return result.toString().trim();
    }
    
    /**
     * Process a category for evaluation.
     */
    private EvaluationCategory processCategory(Evaluation evaluation, GridCategory category, 
                                             Map<String, Object> userVariables, boolean hasSpouse,
                                             EvaluationInsights insights) {
        logger.debug("Processing category: {}", category.getCategoryName());
        
        insights.detailedNotes.append("\nPROCESSING CATEGORY: ").append(category.getCategoryName()).append("\n");
        insights.detailedNotes.append("-------------------------------------------\n");
        
        // Create the evaluation category
        EvaluationCategory evalCategory = new EvaluationCategory();
        evalCategory.setCatEvalId(UUID.randomUUID());
        evalCategory.setEvaluationId(evaluation.getEvaluationId());
        evalCategory.setCategoryId(category.getCategoryId());
        evalCategory.setCategoryName(category.getCategoryName());
        evalCategory.setMaxPossibleScore(hasSpouse ? category.getMaxPointsSpouse() : category.getMaxPointsNoSpouse());
        evalCategory.setUserScore(0);  // Will be updated after subcategories
        evalCategory.setCreatedAt(Instant.now());
        evalCategory.setUpdatedAt(Instant.now());
        
        // Save the evaluation category
        evalCategory = evaluationCategoryRepository.save(evalCategory);
        
        // Process all subcategories in the category
        List<GridSubcategory> subcategories = gridSubcategoryRepository.findByCategoryId(category.getCategoryId());
        int categoryScore = 0;
        
        // Track the original gridSubcategories for later use in dynamic cap calculation
        Map<UUID, GridSubcategory> gridSubcategoryMap = new HashMap<>();
        for (GridSubcategory subcategory : subcategories) {
            gridSubcategoryMap.put(subcategory.getSubcategoryId(), subcategory);
            EvaluationSubcategory evalSubcategory = processSubcategory(evalCategory, subcategory, 
                                                                      userVariables, hasSpouse, insights);
            categoryScore += evalSubcategory.getUserScore();
            
            // Add subcategory details to the insights
            if (evalSubcategory.getUserScore() > 0) {
                insights.addCategoryHighlight(category.getCategoryName(), 
                    String.format("Subcategory '%s' contributed %d points", 
                    subcategory.getSubcategoryName(), evalSubcategory.getUserScore()));
            }
        }
        
        // Check if this is the Skill Transferability category and apply special capping rules
        if (category.getCategoryName().contains("Skill Transferability")) {
            // Use the specialized service for Skill Transferability capping
            // Initialize CappingDetails to collect information
            SkillTransferabilityCappingService.CappingDetails cappingDetails = new SkillTransferabilityCappingService.CappingDetails();
            
            // Apply special capping rules
            int originalScore = categoryScore;
            categoryScore = skillTransferabilityCappingService.applySkillTransferabilityGroupCaps(evalCategory.getCatEvalId(), cappingDetails);
            logger.debug("Applied special Skill Transferability group capping, resulting score: {}", categoryScore);
            
            // If capping was applied, update the evaluation notes
            if (cappingDetails.hasCapping()) {
                // Add the capping details to our insights
                insights.detailedNotes.append(cappingDetails.getDetailedNotes());
                
                if (originalScore > categoryScore) {
                    // Add a capping event
                    insights.addCappingEvent(String.format("Skill Transferability: Points reduced from %d to %d due to group/category caps", 
                        originalScore, categoryScore));
                }
            }
        } else {
            // For other categories, apply the standard dynamic subcategory caps
            int originalScore = categoryScore;
            categoryScore = applyDynamicSubcategoryCaps(evalCategory.getCatEvalId(), gridSubcategoryMap, hasSpouse);
            
            // Check if capping was applied
            if (originalScore > categoryScore) {
                insights.detailedNotes.append(String.format("\nCAPPING APPLIED to %s: Score reduced from %d to %d\n", 
                    category.getCategoryName(), originalScore, categoryScore));
                    
                // Add a capping event
                insights.addCappingEvent(String.format("%s: Points reduced from %d to %d due to subcategory caps", 
                    category.getCategoryName(), originalScore, categoryScore));
            }
        }
        
        // Cap category score at maximum
        int maxScore = evalCategory.getMaxPossibleScore();
        if (categoryScore > maxScore) {
            insights.detailedNotes.append(String.format("\nCATEGORY CAP APPLIED: %s score reduced from %d to %d (category maximum)\n", 
                category.getCategoryName(), categoryScore, maxScore));
                
            // Add a capping event
            insights.addCappingEvent(String.format("%s: Points reduced from %d to %d (category maximum)", 
                category.getCategoryName(), categoryScore, maxScore));
                
            categoryScore = maxScore;
        }
        
        // Update category score
        evalCategory.setUserScore(categoryScore);
        evalCategory = evaluationCategoryRepository.save(evalCategory);
        
        logger.debug("Category {} scored {} points", category.getCategoryName(), categoryScore);
        insights.detailedNotes.append(String.format("\nFinal score for %s: %d out of %d points\n", 
            category.getCategoryName(), categoryScore, maxScore));
            
        return evalCategory;
    }
    
    /**
     * Process a subcategory for evaluation.
     */
    private EvaluationSubcategory processSubcategory(EvaluationCategory evalCategory, GridSubcategory subcategory,
                                                  Map<String, Object> userVariables, boolean hasSpouse,
                                                  EvaluationInsights insights) {
        logger.debug("Processing subcategory: {}", subcategory.getSubcategoryName());
        
        insights.detailedNotes.append(String.format("\nSubcategory: %s\n", subcategory.getSubcategoryName()));
        
        // Create the evaluation subcategory
        EvaluationSubcategory evalSubcategory = new EvaluationSubcategory();
        evalSubcategory.setSubcatEvalId(UUID.randomUUID());
        evalSubcategory.setCatEvalId(evalCategory.getCatEvalId());
        evalSubcategory.setSubcategoryId(subcategory.getSubcategoryId());
        evalSubcategory.setSubcategoryName(subcategory.getSubcategoryName());
        evalSubcategory.setMaxPossibleScore(hasSpouse ? 
                                          subcategory.getMaxPointsSpouse() : 
                                          subcategory.getMaxPointsNoSpouse());
        evalSubcategory.setUserScore(0);  // Will be updated after fields
        evalSubcategory.setFieldCount(0);  // Will be updated after fields
        evalSubcategory.setCreatedAt(Instant.now());
        evalSubcategory.setUpdatedAt(Instant.now());
        
        // Save the evaluation subcategory
        evalSubcategory = evaluationSubcategoryRepository.save(evalSubcategory);
        
        // Process all fields in the subcategory
        List<GridField> fields = gridFieldRepository.findBySubcategoryId(subcategory.getSubcategoryId());
        
        // Group fields by name to handle mutually exclusive fields
        Map<String, List<GridField>> fieldsByName = new HashMap<>();
        for (GridField field : fields) {
            String fieldName = field.getFieldName();
            if (!fieldsByName.containsKey(fieldName)) {
                fieldsByName.put(fieldName, new ArrayList<>());
            }
            fieldsByName.get(fieldName).add(field);
        }
        
        int subcategoryScore = 0;
        int fieldCount = 0;
        
        // Process each group of fields (fields with the same name)
        for (Map.Entry<String, List<GridField>> entry : fieldsByName.entrySet()) {
            String fieldName = entry.getKey();
            List<GridField> fieldsInGroup = entry.getValue();
            
            int highestPointsForGroup = 0;
            int qualifyingFields = 0;
            
            insights.detailedNotes.append(String.format("  Field group: %s\n", fieldName));
            
            // Process each field in the group
            for (GridField field : fieldsInGroup) {
                EvaluationField evalField = processField(evalSubcategory, field, 
                                                       userVariables, hasSpouse, insights);
                if (evalField.getUserQualifies()) {
                    qualifyingFields++;
                    int points = evalField.getUserPointsEarned();
                    
                    insights.detailedNotes.append(String.format("    - Qualified for: %s, earned %d points\n", 
                        field.getFieldName(), points));
                        
                    if (points > highestPointsForGroup) {
                        highestPointsForGroup = points;
                    }
                }
            }
            
            // Add the highest points for this field group to the subcategory score
            subcategoryScore += highestPointsForGroup;
            if (qualifyingFields > 0) {
                fieldCount++;
            }
            
            if (highestPointsForGroup > 0) {
                insights.detailedNotes.append(String.format("    → Field group contributed %d points\n", highestPointsForGroup));
            } else {
                insights.detailedNotes.append("    → No qualifying fields in this group\n");
            }
            
            logger.debug("Field group '{}' contributed {} points to subcategory score", 
                       fieldName, highestPointsForGroup);
        }
        
        // Cap subcategory score at maximum
        int maxScore = evalSubcategory.getMaxPossibleScore();
        if (subcategoryScore > maxScore) {
            insights.detailedNotes.append(String.format("  SUBCATEGORY CAP APPLIED: Score reduced from %d to %d\n", 
                subcategoryScore, maxScore));
                
            subcategoryScore = maxScore;
        }
        
        // Update subcategory score and field count
        evalSubcategory.setUserScore(subcategoryScore);
        evalSubcategory.setFieldCount(fieldCount);
        evalSubcategory = evaluationSubcategoryRepository.save(evalSubcategory);
        
        logger.debug("Subcategory {} scored {} points", subcategory.getSubcategoryName(), subcategoryScore);
        insights.detailedNotes.append(String.format("  Final subcategory score: %d out of %d points\n", 
            subcategoryScore, maxScore));
            
        return evalSubcategory;
    }
    
    /**
     * Process a field for evaluation.
     */
    private EvaluationField processField(EvaluationSubcategory evalSubcategory, GridField field,
                                       Map<String, Object> userVariables, boolean hasSpouse,
                                       EvaluationInsights insights) {
        logger.debug("Processing field: {}", field.getFieldName());
        
        // Evaluate the logic expression
        boolean qualifies = false;
        String logicExpression = field.getLogicExpression();
        String actualValue = "";
        
        if (logicExpression != null && !logicExpression.isEmpty()) {
            try {
                // Extract variable values for the actual_value field
                actualValue = extractActualValuesFromExpression(logicExpression, userVariables);
                
                // Get the logic operator if present
                String logicOperator = field.getLogicOperator();
                
                // Evaluate the expression for qualification, passing the operator
                qualifies = logicExpressionEvaluator.evaluateLogicExpression(
                    logicExpression, 
                    userVariables,
                    logicOperator
                );
            } catch (IllegalArgumentException | NullPointerException e) {
                logger.error("Error evaluating logic expression: {}", logicExpression, e);
                insights.detailedNotes.append(String.format("    Error evaluating field '%s': %s\n", 
                    field.getFieldName(), e.getMessage()));
            }
        }
        
        // Get the points based on whether the applicant has a spouse
        int points = 0;
        if (qualifies) {
            points = hasSpouse ? field.getPointsWithSpouse() : field.getPointsWithoutSpouse();
        }
        
        // Create the evaluation field
        EvaluationField evalField = new EvaluationField();
        evalField.setFieldEvalId(UUID.randomUUID());
        evalField.setSubcatEvalId(evalSubcategory.getSubcatEvalId());
        evalField.setFieldId(field.getFieldId());
        evalField.setFieldName(field.getFieldName());
        evalField.setLogicExpression(logicExpression);
        evalField.setApplicationId(getApplicationIdFromSubcategory(evalSubcategory));
        evalField.setUserPointsEarned(points);
        evalField.setUserQualifies(qualifies);
        evalField.setActualValue(actualValue);
        evalField.setCreatedAt(Instant.now());
        evalField.setUpdatedAt(Instant.now());
        evalField.setEvaluationDate(Instant.now());
        
        // Save the evaluation field
        evalField = evaluationFieldRepository.save(evalField);
        
        logger.debug("Field {} {} and earned {} points", 
                   field.getFieldName(), qualifies ? "qualified" : "did not qualify", points);
        
        return evalField;
    }
    
    /**
     * Extracts the actual values of variables used in an expression.
     * 
     * @param expression The expression to extract values from
     * @param variables Map of variable names to their values
     * @return A string containing the extracted values
     */
    private String extractActualValuesFromExpression(String expression, Map<String, Object> variables) {
        StringBuilder result = new StringBuilder();
        
        // Check for OR expressions first
        if (expression.contains(OR_OPERATOR)) {
            String[] orParts = expression.split(OR_OPERATOR);
            for (String orPart : orParts) {
                String orValue = extractSingleConditionValue(orPart.trim(), variables);
                if (orValue != null) {
                    if (result.length() > 0) {
                        result.append(EXPRESSION_SEPARATOR).append(" ");
                    }
                    result.append(orValue);
                }
            }
            return result.toString();
        }
        
        // Handle compound expressions (multiple conditions separated by semicolons)
        String[] conditions = expression.split(EXPRESSION_SEPARATOR);
        
        for (String condition : conditions) {
            String trimmedCondition = condition.trim();
            if (trimmedCondition.isEmpty()) {
                continue;
            }
            
            // Handle simple conditions
            String value = extractSingleConditionValue(trimmedCondition, variables);
            if (value != null) {
                if (result.length() > 0) {
                    result.append(EXPRESSION_SEPARATOR).append(" ");
                }
                result.append(value);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Extracts the actual value of a variable used in a single condition.
     * 
     * @param condition The single condition to extract value from
     * @param variables Map of variable names to their values
     * @return A string containing only the actual value of the variable
     */
    private String extractSingleConditionValue(String condition, Map<String, Object> variables) {
        // Use regex to extract the variable name (left side of the condition)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\s*([^=<>!]+)\\s*([=!<>]+|IN|NOT IN|IS|IS NOT).*");
        java.util.regex.Matcher matcher = pattern.matcher(condition);
        
        if (matcher.find()) {
            String variableName = matcher.group(1).trim();
            Object value = variables.get(variableName);
            
            // Return just the value if found, "null" if null, or "EMPTY" if empty
            if (value == null) {
                return "null";
            } else if (value.toString().isEmpty()) {
                return "EMPTY";
            } else {
                return value.toString();
            }
        }
        
        return "EMPTY";
    }
    
    /**
     * Helper method to get the application ID from a subcategory.
     */
    private UUID getApplicationIdFromSubcategory(EvaluationSubcategory subcategory) {
        UUID catEvalId = subcategory.getCatEvalId();
        EvaluationCategory category = evaluationCategoryRepository.findById(catEvalId)
                .orElseThrow(() -> new IllegalStateException("Category not found for subcategory"));
        
        UUID evaluationId = category.getEvaluationId();
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new IllegalStateException("Evaluation not found for category"));
        
        return evaluation.getApplicationId();
    }
    
    /**
     * Applies dynamic capping rules to subcategories based on their max_possible_score values.
     * This ensures that specific subcategory groups don't exceed their defined limits
     * by using the actual max scores from the grid schema.
     * 
     * @param catEvalId The category evaluation ID
     * @param gridSubcategoryMap Map of grid subcategory ID to GridSubcategory objects
     * @param hasSpouse Whether the applicant has a spouse
     * @return The updated total score for the category
     */
    private int applyDynamicSubcategoryCaps(UUID catEvalId, Map<UUID, GridSubcategory> gridSubcategoryMap, boolean hasSpouse) {
        List<EvaluationSubcategory> allSubcategories = evaluationSubcategoryRepository.findByCatEvalId(catEvalId);
        
        // Group subcategories by their parent subcategory ID
        Map<UUID, List<EvaluationSubcategory>> subcategoryGroups = new HashMap<>();
        Map<UUID, Integer> groupMaxScores = new HashMap<>();
        
        // First pass: Identify all unique subcategory IDs to create groups
        for (EvaluationSubcategory subcategory : allSubcategories) {
            UUID subcatId = subcategory.getSubcategoryId();
            
            // Create a group for each subcategory
            if (!subcategoryGroups.containsKey(subcatId)) {
                subcategoryGroups.put(subcatId, new ArrayList<>());
                groupMaxScores.put(subcatId, 0);
            }
            
            // Add subcategory to its own group (by ID)
            subcategoryGroups.get(subcatId).add(subcategory);
            
            // Set the max score for this group based on the original GridSubcategory data
            GridSubcategory gridSubcat = gridSubcategoryMap.get(subcatId);
            if (gridSubcat != null) {
                // Get max score based on spouse status
                int maxPoints = hasSpouse ? gridSubcat.getMaxPointsSpouse() : gridSubcat.getMaxPointsNoSpouse();
                groupMaxScores.put(subcatId, maxPoints);
            }
        }
        
        // Apply caps to each group based on the dynamically calculated maximums
        for (Map.Entry<UUID, List<EvaluationSubcategory>> entry : subcategoryGroups.entrySet()) {
            UUID groupId = entry.getKey();
            List<EvaluationSubcategory> groupSubcategories = entry.getValue();
            
            if (groupSubcategories.isEmpty()) {
                continue;
            }
            
            // Get the group cap (max possible score for this group)
            int groupCap = groupMaxScores.get(groupId);
            
            // Sum the actual scores for this group
            int groupTotalScore = groupSubcategories.stream()
                .mapToInt(EvaluationSubcategory::getUserScore)
                .sum();
            
            // Apply capping if needed
            if (groupTotalScore > groupCap) {
                String groupName = groupSubcategories.get(0).getSubcategoryName(); // For logging
                logger.info("[DYNAMIC GROUP CAPPING] Group '{}' total score ({}) exceeds cap of {} - applying cap", 
                    groupName, groupTotalScore, groupCap);
                
                // Apply proportional reduction to maintain relative scores
                double scaleFactor = (double) groupCap / groupTotalScore;
                
                for (EvaluationSubcategory subcat : groupSubcategories) {
                    int originalScore = subcat.getUserScore();
                    // Use proper rounding instead of floor to avoid potentially losing points
                    int newScore = (int) Math.round(originalScore * scaleFactor);
                    
                    // Update subcategory score
                    subcat.setUserScore(newScore);
                    evaluationSubcategoryRepository.save(subcat);
                    
                    logger.info("[DYNAMIC GROUP CAPPING] Adjusted subcategory '{}' score from {} to {}", 
                        subcat.getSubcategoryName(), originalScore, newScore);
                }
            }
        }
        
        // Recalculate the category score after applying all group caps
        return allSubcategories.stream()
            .mapToInt(EvaluationSubcategory::getUserScore)
            .sum();
    }

    /**
     * Retrieves user variables for evaluation from the application data.
     * Fetches real user profile data from the repository.
     * 
     * @param applicationId The ID of the application to get variables for
     * @return Map of variable names to their values
     */
    public Map<String, Object> getUserVariables(UUID applicationId) {
        logger.info("Getting user variables for application: {}", applicationId);
        
        // Fetch the user profile from the repository
        UserImmigrationProfile profile = profileRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for application: " + applicationId));
        
        logger.info("[PROFILE DATA] ApplicationId={}, Name={}, Age={}, Citizenship={}, Education={}, MaritalStatus={}",
                profile.getApplicationId(),
                profile.getApplicantName(),
                profile.getApplicantAge(),
                profile.getApplicantCitizenship(),
                profile.getApplicantEducationLevel(),
                profile.getApplicantMaritalStatus());
        
        // Generate variables dynamically from profile properties
        Map<String, Object> variables = generateVariablesFromProfile(profile);
        
        // Add computed/derived values that aren't direct properties
        enrichVariablesWithDerivedValues(variables, profile);
        
        logger.debug("Generated {} user variables for application {}", variables.size(), applicationId);
        return variables;
    }
    
    /**
     * Dynamically generates variables from the profile object using reflection.
     * Converts getter method names to snake_case variable names.
     * 
     * @param profile The user immigration profile
     * @return Map of variable names (in snake_case) to their values
     */
    private Map<String, Object> generateVariablesFromProfile(UserImmigrationProfile profile) {
        Map<String, Object> variables = new HashMap<>();
        
        // Get all methods from the profile class
        for (Method method : UserImmigrationProfile.class.getMethods()) {
            String methodName = method.getName();
            
            // Only process getter methods (starting with "get" or "is" and having no parameters)
            if ((methodName.startsWith(GET_PREFIX) || methodName.startsWith(IS_PREFIX)) && 
                method.getParameterCount() == 0) {
                
                try {
                    // Invoke the method to get the value
                    Object value = method.invoke(profile);
                    
                    // Skip if value is null, complex objects, or getClass()
                    if (value == null || methodName.equals("getClass") || 
                        value instanceof Collection || value instanceof Map ||
                        value instanceof User) {
                        continue;
                    }
                    
                    // Convert method name to variable name (remove prefix and convert to snake_case)
                    String variableName;
                    if (methodName.startsWith(GET_PREFIX)) {
                        variableName = methodName.substring(GET_PREFIX_LENGTH); // Remove "get" prefix
                    } else {
                        variableName = methodName.substring(IS_PREFIX_LENGTH); // Remove "is" prefix
                    }
                    
                    // Convert from camelCase to snake_case
                    variableName = camelToSnakeCase(variableName);
                    
                    // Add to variables map
                    variables.put(variableName, value);
                    
                } catch (IllegalAccessException | InvocationTargetException e) {
                    logger.warn("Failed to get value for method {}: {}", methodName, e.getMessage());
                }
            }
        }
        
        return variables;
    }
    
    /**
     * Converts a camelCase string to snake_case.
     * Example: "applicantAge" becomes "applicant_age"
     * 
     * @param camelCase The camelCase string to convert
     * @return The snake_case version of the string
     */
    private String camelToSnakeCase(String camelCase) {
        // Insert underscore before uppercase letters and convert to lowercase
        String regex = "([a-z])([A-Z])";
        String replacement = "$1_$2";
        return camelCase.replaceAll(regex, replacement).toLowerCase();
    }
    
    /**
     * Enriches the variables map with derived/computed values that aren't 
     * direct properties of the profile.
     * 
     * @param variables The variables map to enrich
     * @param profile The user immigration profile
     */
    private void enrichVariablesWithDerivedValues(Map<String, Object> variables, UserImmigrationProfile profile) {
        // These scores are already CLB scores according to user (no conversion needed)
        // Primary language test scores
        variables.put("primary_clb_speaking", profile.getPrimaryTestSpeakingScore());
        variables.put("primary_clb_listening", profile.getPrimaryTestListeningScore());
        variables.put("primary_clb_reading", profile.getPrimaryTestReadingScore());
        variables.put("primary_clb_writing", profile.getPrimaryTestWritingScore());
        
        // Add lowest CLB score (used in many calculations)
        int lowestPrimaryCLB = Math.min(
            Math.min(profile.getPrimaryTestSpeakingScore(), profile.getPrimaryTestListeningScore()),
            Math.min(profile.getPrimaryTestReadingScore(), profile.getPrimaryTestWritingScore())
        );
        variables.put("primary_clb_score", lowestPrimaryCLB);
        
        // Add secondary language test scores if available
        if (profile.isTookSecondaryLanguageTest() && profile.getSecondaryTestType() != null) {
            if (profile.getSecondaryTestSpeakingScore() != null) {
                variables.put("secondary_clb_speaking", profile.getSecondaryTestSpeakingScore());
            }
            if (profile.getSecondaryTestListeningScore() != null) {
                variables.put("secondary_clb_listening", profile.getSecondaryTestListeningScore());
            }
            if (profile.getSecondaryTestReadingScore() != null) {
                variables.put("secondary_clb_reading", profile.getSecondaryTestReadingScore());
            }
            if (profile.getSecondaryTestWritingScore() != null) {
                variables.put("secondary_clb_writing", profile.getSecondaryTestWritingScore());
            }
            
            // Add lowest secondary CLB score if all scores are available
            if (profile.getSecondaryTestSpeakingScore() != null && 
                profile.getSecondaryTestListeningScore() != null &&
                profile.getSecondaryTestReadingScore() != null &&
                profile.getSecondaryTestWritingScore() != null) {
                
                int lowestSecondaryCLB = Math.min(
                    Math.min(profile.getSecondaryTestSpeakingScore(), profile.getSecondaryTestListeningScore()),
                    Math.min(profile.getSecondaryTestReadingScore(), profile.getSecondaryTestWritingScore())
                );
                variables.put("secondary_clb_score", lowestSecondaryCLB);
            }
        }
        
        // Add partner language test scores if available
        if (hasSpouse(profile) && profile.getPartnerLanguageTestType() != null) {
            if (profile.getPartnerTestSpeakingScore() != null) {
                variables.put("partner_clb_speaking", profile.getPartnerTestSpeakingScore());
            }
            if (profile.getPartnerTestListeningScore() != null) {
                variables.put("partner_clb_listening", profile.getPartnerTestListeningScore());
            }
            if (profile.getPartnerTestReadingScore() != null) {
                variables.put("partner_clb_reading", profile.getPartnerTestReadingScore());
            }
            if (profile.getPartnerTestWritingScore() != null) {
                variables.put("partner_clb_writing", profile.getPartnerTestWritingScore());
            }
            
            // Add lowest partner CLB score if all scores are available
            if (profile.getPartnerTestSpeakingScore() != null && 
                profile.getPartnerTestListeningScore() != null &&
                profile.getPartnerTestReadingScore() != null &&
                profile.getPartnerTestWritingScore() != null) {
                
                int lowestPartnerCLB = Math.min(
                    Math.min(profile.getPartnerTestSpeakingScore(), profile.getPartnerTestListeningScore()),
                    Math.min(profile.getPartnerTestReadingScore(), profile.getPartnerTestWritingScore())
                );
                variables.put("partner_clb_score", lowestPartnerCLB);
            }
        }
        
        // Any other derived values that need special handling
    }
    
    /**
     * Checks if the profile indicates the applicant has a spouse.
     * 
     * @param profile The user immigration profile
     * @return True if the applicant has a spouse, false otherwise
     */
    private boolean hasSpouse(UserImmigrationProfile profile) {
        String maritalStatus = profile.getApplicantMaritalStatus();
        return maritalStatus != null && 
               (maritalStatus.equalsIgnoreCase(MARITAL_STATUS_MARRIED) || 
                maritalStatus.equalsIgnoreCase(MARITAL_STATUS_COMMON_LAW));
    }

    /**
     * Determines if the applicant has a spouse based on application data.
     * 
     * @param applicationId The ID of the application to check
     * @return True if the applicant has a spouse, false otherwise
     */
    public boolean hasSpouse(UUID applicationId) {
        // Fetch the profile to check marital status
        UserImmigrationProfile profile = profileRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for application: " + applicationId));
        
        return hasSpouse(profile);
    }
} 