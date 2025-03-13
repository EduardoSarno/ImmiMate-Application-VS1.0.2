package co.immimate.profile.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import co.immimate.profile.dto.ProfileSubmissionRequest;

/**
 * Validator implementation for the {@link SecondaryLanguageConsistency} annotation.
 * Validates that if secondary language test is indicated, all related fields are provided.
 */
public class SecondaryLanguageConsistencyValidator implements ConstraintValidator<SecondaryLanguageConsistency, ProfileSubmissionRequest> {

    @Override
    public void initialize(SecondaryLanguageConsistency constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(ProfileSubmissionRequest request, ConstraintValidatorContext context) {
        // If secondary language test is not indicated, validation passes
        if (request.getTookSecondaryLanguageTest() == null || !request.getTookSecondaryLanguageTest()) {
            return true;
        }
        
        // If secondary language test is indicated, all related fields must be provided
        boolean isValid = true;
        
        // Disable default error message
        context.disableDefaultConstraintViolation();
        
        // Check secondary test type
        if (request.getSecondaryTestType() == null || request.getSecondaryTestType().trim().isEmpty()) {
            context.buildConstraintViolationWithTemplate("Secondary language test type is required when secondary test is indicated")
                   .addPropertyNode("secondaryTestType")
                   .addConstraintViolation();
            isValid = false;
        }
        
        // Check secondary test speaking score
        if (request.getSecondaryTestSpeakingScore() == null) {
            context.buildConstraintViolationWithTemplate("Secondary language speaking score is required when secondary test is indicated")
                   .addPropertyNode("secondaryTestSpeakingScore")
                   .addConstraintViolation();
            isValid = false;
        }
        
        // Check secondary test listening score
        if (request.getSecondaryTestListeningScore() == null) {
            context.buildConstraintViolationWithTemplate("Secondary language listening score is required when secondary test is indicated")
                   .addPropertyNode("secondaryTestListeningScore")
                   .addConstraintViolation();
            isValid = false;
        }
        
        // Check secondary test reading score
        if (request.getSecondaryTestReadingScore() == null) {
            context.buildConstraintViolationWithTemplate("Secondary language reading score is required when secondary test is indicated")
                   .addPropertyNode("secondaryTestReadingScore")
                   .addConstraintViolation();
            isValid = false;
        }
        
        // Check secondary test writing score
        if (request.getSecondaryTestWritingScore() == null) {
            context.buildConstraintViolationWithTemplate("Secondary language writing score is required when secondary test is indicated")
                   .addPropertyNode("secondaryTestWritingScore")
                   .addConstraintViolation();
            isValid = false;
        }
        
        return isValid;
    }
} 