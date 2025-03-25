package co.immimate.auth.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user registration request
 * At least one of email or phoneNumber must be provided
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    
    // Validation message constants
    private static final String EMAIL_VALID_MESSAGE = "Email should be valid";
    private static final String PASSWORD_REQUIRED_MESSAGE = "Password is required";
    private static final String PASSWORD_SIZE_MESSAGE = "Password must be at least 6 characters";
    private static final String FIRST_NAME_REQUIRED_MESSAGE = "First name is required";
    private static final String LAST_NAME_REQUIRED_MESSAGE = "Last name is required";
    private static final String PHONE_FORMAT_MESSAGE = "Phone number should contain only digits";
    
    // Validation constants
    private static final int PASSWORD_MIN_LENGTH = 6;
    private static final String PHONE_REGEX = "^\\+?[0-9\\s]+$";
    
    @Email(message = EMAIL_VALID_MESSAGE)
    private String email;
    
    @NotBlank(message = PASSWORD_REQUIRED_MESSAGE)
    @Size(min = PASSWORD_MIN_LENGTH, message = PASSWORD_SIZE_MESSAGE)
    private String password;
    
    @NotBlank(message = FIRST_NAME_REQUIRED_MESSAGE)
    private String firstName;
    
    @NotBlank(message = LAST_NAME_REQUIRED_MESSAGE)
    private String lastName;
    
    @Pattern(regexp = PHONE_REGEX, message = PHONE_FORMAT_MESSAGE)
    private String phoneNumber;
    
    /**
     * Validates that at least one of email or phoneNumber is provided
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return (email != null && !email.trim().isEmpty()) || 
               (phoneNumber != null && !phoneNumber.trim().isEmpty());
    }
} 