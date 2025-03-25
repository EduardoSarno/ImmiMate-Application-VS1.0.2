package co.immimate.auth.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for login request containing email and password
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {
    
    // Validation message constants
    private static final String EMAIL_REQUIRED_MESSAGE = "Email is required";
    private static final String EMAIL_VALID_MESSAGE = "Email should be valid";
    private static final String PASSWORD_REQUIRED_MESSAGE = "Password is required";
    
    @NotBlank(message = EMAIL_REQUIRED_MESSAGE)
    @Email(message = EMAIL_VALID_MESSAGE)
    private String email;
    
    @NotBlank(message = PASSWORD_REQUIRED_MESSAGE)
    private String password;
} 