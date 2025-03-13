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
    
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    @Pattern(regexp = "^\\+?[0-9\\s]+$", message = "Phone number should contain only digits")
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