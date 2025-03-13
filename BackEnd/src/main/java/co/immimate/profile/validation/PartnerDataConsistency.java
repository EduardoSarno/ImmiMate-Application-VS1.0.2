package co.immimate.profile.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Validates that partner data is consistent with marital status.
 * If applicant is single, partner data should not be provided.
 * This is a class-level annotation that performs cross-field validation.
 */
@Documented
@Constraint(validatedBy = PartnerDataConsistencyValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface PartnerDataConsistency {
    
    String message() default "Partner data should not be provided for single applicants";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
} 