package co.immimate.profile.dto;

import java.util.UUID;

import javax.validation.constraints.Email;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.PositiveOrZero;

import co.immimate.profile.validation.PartnerDataConsistency;
import co.immimate.profile.validation.SecondaryLanguageConsistency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for profile submission requests.
 * Contains comprehensive validation annotations to ensure data integrity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SecondaryLanguageConsistency
@PartnerDataConsistency
public class ProfileSubmissionRequest {
    
    private UUID userId; // Optional, can be found by email
    
    private String googleId; // Google ID from OAuth for alternative identification
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String userEmail;
    
    @NotBlank(message = "Applicant name is required")
    private String applicantName;
    
    @NotNull(message = "Age is required")
    @Min(value = 16, message = "Age must be at least 16")
    @Max(value = 120, message = "Age must be realistic (maximum 120)")
    private Integer applicantAge;
    
    @NotBlank(message = "Citizenship is required")
    private String applicantCitizenship;
    
    @NotBlank(message = "Country of residence is required")
    private String applicantResidence;
    
    @NotBlank(message = "Marital status is required")
    @Pattern(regexp = "^(Single|Married|Common-Law|Separated|Divorced|Widowed)$", 
             message = "Marital status must be one of: Single, Married, Common-Law, Separated, Divorced, Widowed")
    private String applicantMaritalStatus;
    
    @NotBlank(message = "Education level is required")
    private String applicantEducationLevel;
    
    private Boolean educationCompletedInCanada;
    private String canadianEducationLevel;
    private Boolean hasEducationalCredentialAssessment;
    
    @NotBlank(message = "Primary language test type is required")
    @Pattern(regexp = "^(IELTS|CELPIP|TEF|TCF|PTE)$", 
             message = "Language test type must be one of: IELTS, CELPIP, TEF, TCF, PTE")
    private String primaryLanguageTestType;
    
    @NotNull(message = "Speaking score is required")
    @Min(value = 1, message = "Speaking CLB score must be between 1 and 12")
    @Max(value = 12, message = "Speaking CLB score must be between 1 and 12")
    private Integer primaryTestSpeakingScore;
    
    @NotNull(message = "Listening score is required")
    @Min(value = 1, message = "Listening CLB score must be between 1 and 12")
    @Max(value = 12, message = "Listening CLB score must be between 1 and 12")
    private Integer primaryTestListeningScore;
    
    @NotNull(message = "Reading score is required")
    @Min(value = 1, message = "Reading CLB score must be between 1 and 12")
    @Max(value = 12, message = "Reading CLB score must be between 1 and 12")
    private Integer primaryTestReadingScore;
    
    @NotNull(message = "Writing score is required")
    @Min(value = 1, message = "Writing CLB score must be between 1 and 12")
    @Max(value = 12, message = "Writing CLB score must be between 1 and 12")
    private Integer primaryTestWritingScore;
    
    // Original test scores (before CLB conversion)
    private String primaryTestSpeakingOriginal;
    private String primaryTestListeningOriginal;
    private String primaryTestReadingOriginal;
    private String primaryTestWritingOriginal;
    
    private Boolean tookSecondaryLanguageTest;
    
    private String secondaryTestType;
    
    @Min(value = 1, message = "Secondary speaking CLB score must be between 1 and 12")
    @Max(value = 12, message = "Secondary speaking CLB score must be between 1 and 12")
    private Integer secondaryTestSpeakingScore;
    
    @Min(value = 1, message = "Secondary listening CLB score must be between 1 and 12")
    @Max(value = 12, message = "Secondary listening CLB score must be between 1 and 12")
    private Integer secondaryTestListeningScore;
    
    @Min(value = 1, message = "Secondary reading CLB score must be between 1 and 12")
    @Max(value = 12, message = "Secondary reading CLB score must be between 1 and 12")
    private Integer secondaryTestReadingScore;
    
    @Min(value = 1, message = "Secondary writing CLB score must be between 1 and 12")
    @Max(value = 12, message = "Secondary writing CLB score must be between 1 and 12")
    private Integer secondaryTestWritingScore;
    
    // Secondary test original scores
    private String secondaryTestSpeakingOriginal;
    private String secondaryTestListeningOriginal;
    private String secondaryTestReadingOriginal;
    private String secondaryTestWritingOriginal;
    
    // Employment history
    @NotNull(message = "Years of Canadian work experience required")
    @PositiveOrZero(message = "Years must be 0 or positive")
    private Integer canadianWorkExperienceYears;
    
    private Integer nocCodeCanadian;
    
    private Integer canadianOccupationTeerCategory;
    
    @NotNull(message = "Years of foreign work experience required")
    @PositiveOrZero(message = "Years must be 0 or positive")
    private Integer foreignWorkExperienceYears;
    
    private Integer nocCodeForeign;
    
    private Integer foreignOccupationTeerCategory;
    
    private Boolean workingInCanada;
    
    private Boolean hasProvincialNomination;
    
    @NotBlank(message = "Province of interest is required")
    private String provinceOfInterest;
    
    private Boolean hasCanadianRelatives;
    private String relationshipWithCanadianRelative;
    private Boolean receivedInvitationToApply;
    
    @NotNull(message = "Settlement funds are required")
    @PositiveOrZero(message = "Settlement funds cannot be negative")
    private Integer settlementFundsCAD;
    
    @NotBlank(message = "Preferred city is required")
    private String preferredCity;
    
    @NotBlank(message = "Preferred destination province is required")
    private String preferredDestinationProvince;
    
    private String partnerEducationLevel;
    private String partnerLanguageTestType;
    
    @Min(value = 1, message = "Partner speaking CLB score must be between 1 and 12")
    @Max(value = 12, message = "Partner speaking CLB score must be between 1 and 12")
    private Integer partnerTestSpeakingScore;
    
    @Min(value = 1, message = "Partner listening CLB score must be between 1 and 12")
    @Max(value = 12, message = "Partner listening CLB score must be between 1 and 12")
    private Integer partnerTestListeningScore;
    
    @Min(value = 1, message = "Partner reading CLB score must be between 1 and 12")
    @Max(value = 12, message = "Partner reading CLB score must be between 1 and 12")
    private Integer partnerTestReadingScore;
    
    @Min(value = 1, message = "Partner writing CLB score must be between 1 and 12")
    @Max(value = 12, message = "Partner writing CLB score must be between 1 and 12")
    private Integer partnerTestWritingScore;
    
    // Partner original scores
    private String partnerTestSpeakingOriginal;
    private String partnerTestListeningOriginal;
    private String partnerTestReadingOriginal;
    private String partnerTestWritingOriginal;
    
    @PositiveOrZero(message = "Partner Canadian work experience years cannot be negative")
    private Integer partnerCanadianWorkExperienceYears;
    
    private Integer spouseOccupationTeerCategory;
    
    private Boolean hasJobOffer;
    private Boolean isJobOfferLmiaApproved;
    
    @PositiveOrZero(message = "Job offer wage cannot be negative")
    private Integer jobOfferWageCAD;
    
    private Integer jobOfferNocCode;
    
    private Integer jobofferOccupationTeerCategory;
    
    private String jobOfferWeeklyHours;
    
    private Boolean tradesCertification;
    
    // Store the complete JSON payload for future reference or additional fields
    @NotBlank(message = "JSON payload is required")
    private String jsonPayload;
} 