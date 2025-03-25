package co.immimate.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * Immutable Data Transfer Object for profile submission responses.
 * Uses Lombok's @Value to create an immutable class with final fields.
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class ProfileSubmissionResponse {
    boolean success;
    String message;
    UUID profileId;
    Integer eligibilityScore;
    String profileStatus;
    
    // Default constructor needed for frameworks (Jackson deserialization)
    // Protected to discourage direct usage
    protected ProfileSubmissionResponse() {
        this.success = false;
        this.message = null;
        this.profileId = null;
        this.eligibilityScore = null;
        this.profileStatus = null;
    }
    
    // Static factory methods for common responses
    public static ProfileSubmissionResponse success(UUID profileId, String message) {
        return ProfileSubmissionResponse.builder()
                .success(true)
                .message(message)
                .profileId(profileId)
                .build();
    }
    
    public static ProfileSubmissionResponse success(UUID profileId, String message, Integer score) {
        return ProfileSubmissionResponse.builder()
                .success(true)
                .message(message)
                .profileId(profileId)
                .eligibilityScore(score)
                .build();
    }
    
    public static ProfileSubmissionResponse error(String message) {
        return ProfileSubmissionResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
} 