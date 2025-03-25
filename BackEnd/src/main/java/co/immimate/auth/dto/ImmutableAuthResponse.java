package co.immimate.auth.dto;

import lombok.Builder;
import lombok.Value;
import lombok.With;

/**
 * Immutable Data Transfer Object for authentication responses.
 * Uses Lombok's @Value to create an immutable class with final fields.
 * This is a demonstration of proper immutability pattern for DTOs.
 */
@Value
@Builder
public class ImmutableAuthResponse {
    String token;
    String email;
    Long userId;
    String role;
    @With // Allows creating a new instance with a modified value
    boolean rememberMe;
    
    /**
     * Static factory method to create a new authentication response.
     * 
     * @param token JWT token
     * @param email User email
     * @param userId User ID
     * @param role User role
     * @return New immutable authentication response
     */
    public static ImmutableAuthResponse of(String token, String email, Long userId, String role) {
        return ImmutableAuthResponse.builder()
                .token(token)
                .email(email)
                .userId(userId)
                .role(role)
                .rememberMe(false)
                .build();
    }
    
    /**
     * Static factory method to create a new authentication response with remember me.
     * 
     * @param token JWT token
     * @param email User email
     * @param userId User ID
     * @param role User role
     * @param rememberMe Whether to remember the user
     * @return New immutable authentication response
     */
    public static ImmutableAuthResponse of(String token, String email, Long userId, String role, boolean rememberMe) {
        return ImmutableAuthResponse.builder()
                .token(token)
                .email(email)
                .userId(userId)
                .role(role)
                .rememberMe(rememberMe)
                .build();
    }
} 