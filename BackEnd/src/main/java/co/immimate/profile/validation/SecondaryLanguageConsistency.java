package co.immimate.profile.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Validates that if secondary language test is indicated, all related fields are provided.
 * This is a class-level annotation that performs cross-field validation.
 */
@Documented
@Constraint(validatedBy = SecondaryLanguageConsistencyValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface SecondaryLanguageConsistency {
    
    String message() default "When secondary language test is indicated, all test scores must be provided";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
} 