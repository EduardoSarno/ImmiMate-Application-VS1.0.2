package co.immimate.scoring.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import co.immimate.profile.model.UserImmigrationProfile;
import co.immimate.profile.repository.ProfileRepository;
import co.immimate.scoring.fields.GridField;
import co.immimate.scoring.fields.GridFieldRepository;
import co.immimate.scoring.model.Evaluation;
import co.immimate.scoring.repository.EvaluationRepository;

/**
 * Service responsible for evaluating user profiles against scoring criteria.
 * Performs dynamic evaluation of scoring rules stored in the database.
 */
@Service
public class EvaluationScoringService {

    private final ProfileRepository profileRepository;
    private final EvaluationRepository evaluationRepository;
    private final GridFieldRepository gridFieldRepository;
    private static final Logger log = LoggerFactory.getLogger(EvaluationScoringService.class);

    /**
     * Constructor with required dependencies.
     */
    public EvaluationScoringService(ProfileRepository profileRepo, EvaluationRepository evalRepo, 
                                    GridFieldRepository gridFieldRepo) 
        {
        this.profileRepository = profileRepo;
        this.evaluationRepository = evalRepo;
        this.gridFieldRepository = gridFieldRepo;
        }

    /**
     * Evaluates a profile against a specific scoring grid.
     * Fetches the profile and scoring rules, then calculates the total score.
     *
     * @param applicationId The ID of the application to evaluate
     * @param gridName The name of the scoring grid to use
     * @return An Evaluation object containing the score
     */
    public Evaluation evaluateProfile(UUID applicationId, String gridName) 
    {
        System.out.println("\n[EvaluationScoringService] Evaluating profile " + applicationId + 
                           " using grid: " + gridName);
        
        // 1. Fetch the user profile
        UserImmigrationProfile profile = profileRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        System.out.println("[EvaluationScoringService] Fetched profile: " + profile);

        // 2. Retrieve all fields from the static DB for this grid
        ArrayList<GridField> fields = new ArrayList<>(gridFieldRepository.findAllByGridName(gridName));
        System.out.println("[EvaluationScoringService] Found " + fields.size() +
                           " fields for grid: " + gridName);

        // 3. Loop over each field & evaluate logic
        int totalScore = 0;
        boolean isMarried = profile.getApplicantMaritalStatus().equalsIgnoreCase("married");

        for (GridField field : fields) {
            boolean qualifies = evaluateLogicExpression(field.getLogicExpression(), profile);

            if (qualifies) {
                int pointsToAdd = isMarried ? field.getPointsWithSpouse() : field.getPointsWithoutSpouse();
                totalScore += pointsToAdd;

                System.out.println("  -> [MATCH] Field: " + field.getFieldName() +
                                   ", Expression: " + field.getLogicExpression() +
                                   ", Points: " + pointsToAdd);
            } else {
                System.out.println("  -> [NO MATCH] Field: " + field.getFieldName() +
                                   ", Expression: " + field.getLogicExpression());
            }
        }

        // 4. Create an Evaluation and save it
        Evaluation eval = new Evaluation(
                UUID.randomUUID(),
                applicationId,
                gridName,
                totalScore,
                Instant.now()
        );

        System.out.println("\n[EvaluationScoringService] Final score: " + totalScore);
        
        
        return eval; // Simply return the Evaluation object without saving
    }

    /**
     * Evaluates a logical expression against a profile.
     * Expressions can contain multiple conditions separated by semicolons.
     * The logic operator determines how conditions are combined.
     *
     * @param expression The logical expression to evaluate
     * @param profile The profile to evaluate against
     * @return True if the expression is satisfied, false otherwise
     */
    private boolean evaluateLogicExpression(String expression, UserImmigrationProfile profile) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }

        // Split by semicolon to get individual conditions
        String[] conditions = expression.split(";");
        
        // If no conditions, return false
        if (conditions.length == 0) {
            return false;
        }
        
        // Default: All conditions must be true (AND logic)
        boolean result = true;
        
        for (String condition : conditions) {
            boolean conditionResult = checkCondition(condition.trim(), profile);
            
            // Short circuit on first false condition (AND logic)
            if (!conditionResult) {
                result = false;
                break;
            }
        }
        
        return result;
    }

    /**
     * Checks a single condition against a profile.
     * Supports conditions like "field_name >= value" or "field_name = value".
     *
     * @param condition The condition to check
     * @param profile The profile to check against
     * @return True if the condition is satisfied, false otherwise
     */
    private boolean checkCondition(String condition, UserImmigrationProfile profile) {
        log.debug("Checking condition: {}", condition);
    
        try {
            // 1️⃣ Handle "IN (...)" condition format
            if (condition.contains(" IN (")) {
                String[] parts = condition.split(" IN \\(");
                if (parts.length != 2) {
                    System.err.println("Unsupported condition format: " + condition);
                    return false;
                }
    
                String fieldName = parts[0].trim();
                String valuesString = parts[1].replace(")", "").trim();
                String[] valueArray = valuesString.split(",");
    
                // 2️⃣ Determine whether the list contains numbers or strings
                boolean isNumericList = Arrays.stream(valueArray).allMatch(v -> v.trim().matches("\\d+"));
    
                if (isNumericList) {
                    // Convert to List of Integers
                    List<Integer> validValues = Arrays.stream(valueArray)
                            .map(String::trim)
                            .map(Integer::parseInt)
                            .toList();
    
                    int fieldValue = checkNumericField(fieldName, profile);
                    return validValues.contains(fieldValue);
                } else {
                    // Convert to List of Strings
                    List<String> validValues = Arrays.stream(valueArray)
                            .map(String::trim)
                            .toList();
    
                    String fieldValue = checkStringField(fieldName, profile);
                    return validValues.contains(fieldValue);
                }
            }
    
            // 3️⃣ Handle Standard Comparisons
            Pattern comparisonPattern = Pattern.compile("(\\w+)\\s*(>=|<=|>|<|==|=|!=)\\s*([\\w\\d\\s'-]+)");
            Matcher matcher = comparisonPattern.matcher(condition);
    
            if (matcher.find()) {
                String fieldName = matcher.group(1);
                String operator = matcher.group(2);
                String valueStr = matcher.group(3).trim();
    
                // Normalize '=' to '=='
                if (operator.equals("=")) {
                    operator = "==";
                }
    
                try {
                    // Numeric comparison
                    if (valueStr.matches("\\d+")) {
                        int fieldValue = checkNumericField(fieldName, profile);
                        int compareValue = Integer.parseInt(valueStr);
    
                        return compareNumeric(fieldValue, compareValue, operator);
                    } else if (valueStr.equalsIgnoreCase("TRUE") || valueStr.equalsIgnoreCase("FALSE")) {
                        // Handle Boolean comparison
                        log.debug("Evaluating boolean condition: {} {} {}", fieldName, operator, valueStr);
                        boolean fieldValue = checkBooleanField(fieldName, profile);
                        boolean compareValue = Boolean.parseBoolean(valueStr);
                        log.debug("Boolean field value: {} (comparing with: {})", fieldValue, compareValue);
                        
                        return compareBoolean(fieldValue, compareValue);
                    } else {
                        // String comparison
                        String fieldValue = checkStringField(fieldName, profile);
    
                        return compareString(fieldValue, valueStr, operator);
                    }
                } catch (Exception e) {
                    System.err.println("Error evaluating condition: " + condition);
                    log.error("Failed to evaluate condition '{}': {}", condition, e.getMessage(), e);
                    return false;
                }
            }
    
            System.err.println("Unsupported condition format: " + condition);
            return false;
        } catch (Exception e) {
            System.err.println("Error checking condition: " + condition);
            log.error("Failed to check condition '{}': {}", condition, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Gets a numeric field value from the profile.
     * Handles special field names and converts them to numeric values.
     *
     * @param fieldName The name of the field to get
     * @param profile The profile to get the field from
     * @return The numeric value of the field
     */
    private int checkNumericField(String fieldName, UserImmigrationProfile profile) {
        switch (fieldName) {
            case "applicant_age" -> {
                return profile.getApplicantAge();
            }
            case "canadian_work_experience_years" -> {
                return profile.getCanadianWorkExperienceYears();
            }
            case "foreign_work_experience_years" -> {
                return profile.getForeignWorkExperienceYears();
            }
            case "primary_test_speaking_score" -> {
                return profile.getPrimaryTestSpeakingScore();
            }
            case "primary_test_listening_score" -> {
                return profile.getPrimaryTestListeningScore();
            }
            case "primary_test_reading_score" -> {
                return profile.getPrimaryTestReadingScore();
            }
            case "primary_test_writing_score" -> {
                return profile.getPrimaryTestWritingScore();
            }
            case "secondary_test_speaking_score" -> {
                return profile.getSecondaryTestSpeakingScore() != null ? profile.getSecondaryTestSpeakingScore() : 0;
            }
            case "secondary_test_listening_score" -> {
                return profile.getSecondaryTestListeningScore() != null ? profile.getSecondaryTestListeningScore() : 0;
            }
            case "secondary_test_reading_score" -> {
                return profile.getSecondaryTestReadingScore() != null ? profile.getSecondaryTestReadingScore() : 0;
            }
            case "secondary_test_writing_score" -> {
                return profile.getSecondaryTestWritingScore() != null ? profile.getSecondaryTestWritingScore() : 0;
            }
            case "noc_code_canadian" -> {
                return profile.getNocCodeCanadian() != null ? profile.getNocCodeCanadian() : 0;
            }
            case "settlement_funds_cad" -> {
                return profile.getSettlementFundsCad();
            }
            case "partner_test_speaking_score" -> {
                return profile.getPartnerTestSpeakingScore() != null ? profile.getPartnerTestSpeakingScore() : 0;
            }
            case "partner_test_listening_score" -> {
                return profile.getPartnerTestListeningScore() != null ? profile.getPartnerTestListeningScore() : 0;
            }
            case "partner_test_reading_score" -> {
                return profile.getPartnerTestReadingScore() != null ? profile.getPartnerTestReadingScore() : 0;
            }
            case "partner_test_writing_score" -> {
                return profile.getPartnerTestWritingScore() != null ? profile.getPartnerTestWritingScore() : 0;
            }
            case "partner_canadian_work_experience_years" -> {
                return profile.getPartnerCanadianWorkExperienceYears() != null ? profile.getPartnerCanadianWorkExperienceYears() : 0;
            }
            case "job_offer_wage_cad" -> {
                return profile.getJobOfferWageCad() != null ? profile.getJobOfferWageCad() : 0;
            }
            default -> throw new IllegalArgumentException("Unknown numeric field: " + fieldName);
        }
    }
    
    /**
     * Gets a string field value from the profile.
     * Handles special field names and retrieves their string values.
     *
     * @param fieldName The name of the field to get
     * @param profile The profile to get the field from
     * @return The string value of the field
     */
    private String checkStringField(String fieldName, UserImmigrationProfile profile) {
        switch (fieldName) {
            case "applicant_citizenship" -> {
                return profile.getApplicantCitizenship();
            }
            case "applicant_residence" -> {
                return profile.getApplicantResidence();
            }
            case "applicant_marital_status" -> {
                return profile.getApplicantMaritalStatus();
            }
            case "applicant_education_level" -> {
                return profile.getApplicantEducationLevel();
            }
            case "canadian_education_level" -> {
                return profile.getCanadianEducationLevel();
            }
            case "primary_language_test_type" -> {
                return profile.getPrimaryLanguageTestType();
            }
            case "secondary_test_type" -> {
                return profile.getSecondaryTestType();
            }
            case "noc_code_foreign" -> {
                return profile.getNocCodeForeign();
            }
            case "province_of_interest" -> {
                return profile.getProvinceOfInterest();
            }
            case "relationship_with_canadian_relative" -> {
                return profile.getRelationshipWithCanadianRelative();
            }
            case "preferred_city" -> {
                return profile.getPreferredCity();
            }
            case "preferred_destination_province" -> {
                return profile.getPreferredDestinationProvince();
            }
            case "partner_education_level" -> {
                return profile.getPartnerEducationLevel();
            }
            case "partner_language_test_type" -> {
                return profile.getPartnerLanguageTestType();
            }
            case "job_offer_noc_code" -> {
                return profile.getJobOfferNocCode();
            }
            default -> throw new IllegalArgumentException("Unknown string field: " + fieldName);
        }
    }
    
    /**
     * Compares two numeric values using the specified operator.
     *
     * @param field The field value
     * @param value The comparison value
     * @param operator The comparison operator
     * @return The result of the comparison
     */
    private boolean compareNumeric(int field, int value, String operator) {
        switch (operator) {
            case ">=" -> {
                return field >= value;
            }
            case "<=" -> {
                return field <= value;
            }
            case ">" -> {
                return field > value;
            }
            case "<" -> {
                return field < value;
            }
            case "==" -> {
                return field == value;
            }
            case "!=" -> {
                return field != value;
            }
            default -> throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
    }
    
    /**
     * Compares two string values using the specified operator.
     *
     * @param field The field value
     * @param value The comparison value
     * @param operator The comparison operator
     * @return The result of the comparison
     */
    private boolean compareString(String field, String value, String operator) {
        if (field == null) {
            return operator.equals("!=") && value != null;
        }
        
        switch (operator) {
            case "==" -> {
                return field.equalsIgnoreCase(value);
            }
            case "!=" -> {
                return !field.equalsIgnoreCase(value);
            }
            case "contains" -> {
                return field.toLowerCase().contains(value.toLowerCase());
            }
            case "startsWith" -> {
                return field.toLowerCase().startsWith(value.toLowerCase());
            }
            case "endsWith" -> {
                return field.toLowerCase().endsWith(value.toLowerCase());
            }
            default -> throw new IllegalArgumentException("Unsupported operator for strings: " + operator);
        }
    }

    private boolean compareBoolean(boolean field, boolean value) {
        return field == value;
    }
    
    private boolean checkBooleanField(String fieldName, UserImmigrationProfile profile) {
        log.debug("Checking boolean field: {}", fieldName);
        boolean result;
        
        switch (fieldName) {
            case "education_completed_in_canada" -> {
                result = profile.getEducationCompletedInCanada() != null ? profile.getEducationCompletedInCanada() : false;
            }
            case "has_educational_credential_assessment" -> {
                result = profile.isHasEducationalCredentialAssessment();
            }
            case "took_secondary_language_test" -> {
                result = profile.isTookSecondaryLanguageTest();
            }
            case "working_in_canada" -> {
                result = profile.isWorkingInCanada();
            }
            case "has_provincial_nomination" -> {
                result = profile.isHasProvincialNomination();
            }
            case "has_canadian_relatives" -> {
                result = profile.isHasCanadianRelatives();
            }
            case "received_invitation_to_apply" -> {
                result = profile.isReceivedInvitationToApply();
            }
            case "has_job_offer" -> {
                result = profile.isHasJobOffer();
            }
            case "is_job_offer_lmia_approved" -> {
                result = profile.getIsJobOfferLmiaApproved() != null ? profile.getIsJobOfferLmiaApproved() : false;
            }
            case "trades_certification" -> {
                result = profile.getTradesCertification() != null ? profile.getTradesCertification() : false;
            }
            default -> {
                log.error("Unknown boolean field: {}", fieldName);
                throw new IllegalArgumentException("Unknown boolean field: " + fieldName);
            }
        }
        
        log.debug("Boolean field {} value: {}", fieldName, result);
        return result;
    }
}
