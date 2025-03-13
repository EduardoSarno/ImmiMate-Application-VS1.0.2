package co.immimate.profile.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import co.immimate.profile.dto.ProfileSubmissionRequest;

/**
 * Validator implementation for the {@link PartnerDataConsistency} annotation.
 * Validates that partner data is consistent with marital status.
 * If applicant is single, partner data should not be provided.
 */
public class PartnerDataConsistencyValidator implements ConstraintValidator<PartnerDataConsistency, ProfileSubmissionRequest> {

    @Override
    public void initialize(PartnerDataConsistency constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(ProfileSubmissionRequest request, ConstraintValidatorContext context) {
        // If marital status is not "Single", validation passes
        if (request.getApplicantMaritalStatus() == null || 
            !request.getApplicantMaritalStatus().equalsIgnoreCase("Single")) {
            return true;
        }
        
        // If applicant is single, partner data should not be provided
        boolean isValid = true;
        
        // Disable default error message
        context.disableDefaultConstraintViolation();
        
        // Check partner education level
        if (request.getPartnerEducationLevel() != null && !request.getPartnerEducationLevel().trim().isEmpty()) {
            context.buildConstraintViolationWithTemplate("Partner education level should not be provided for single applicants")
                   .addPropertyNode("partnerEducationLevel")
                   .addConstraintViolation();
            isValid = false;
        }
        
        // Check partner language test type
        if (request.getPartnerLanguageTestType() != null && !request.getPartnerLanguageTestType().trim().isEmpty()) {
            context.buildConstraintViolationWithTemplate("Partner language test information should not be provided for single applicants")
                   .addPropertyNode("partnerLanguageTestType")
                   .addConstraintViolation();
            isValid = false;
        }
        
        // Check partner test scores
        if (request.getPartnerTestSpeakingScore() != null) {
            context.buildConstraintViolationWithTemplate("Partner language scores should not be provided for single applicants")
                   .addPropertyNode("partnerTestSpeakingScore")
                   .addConstraintViolation();
            isValid = false;
        }
        
        if (request.getPartnerTestListeningScore() != null) {
            context.buildConstraintViolationWithTemplate("Partner language scores should not be provided for single applicants")
                   .addPropertyNode("partnerTestListeningScore")
                   .addConstraintViolation();
            isValid = false;
        }
        
        if (request.getPartnerTestReadingScore() != null) {
            context.buildConstraintViolationWithTemplate("Partner language scores should not be provided for single applicants")
                   .addPropertyNode("partnerTestReadingScore")
                   .addConstraintViolation();
            isValid = false;
        }
        
        if (request.getPartnerTestWritingScore() != null) {
            context.buildConstraintViolationWithTemplate("Partner language scores should not be provided for single applicants")
                   .addPropertyNode("partnerTestWritingScore")
                   .addConstraintViolation();
            isValid = false;
        }
        
        // Check partner work experience - ignore if it's 0 since that's effectively the same as null
        if (request.getPartnerCanadianWorkExperienceYears() != null && 
            request.getPartnerCanadianWorkExperienceYears() > 0) {
            context.buildConstraintViolationWithTemplate("Partner work experience should not be provided for single applicants")
                   .addPropertyNode("partnerCanadianWorkExperienceYears")
                   .addConstraintViolation();
            isValid = false;
        }
        
        return isValid;
    }
} 