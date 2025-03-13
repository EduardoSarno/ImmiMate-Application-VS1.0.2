package co.immimate.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileSubmissionResponse {
    private boolean success;
    private String message;
    private UUID profileId;
    private Integer eligibilityScore;
    private String profileStatus;
    
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