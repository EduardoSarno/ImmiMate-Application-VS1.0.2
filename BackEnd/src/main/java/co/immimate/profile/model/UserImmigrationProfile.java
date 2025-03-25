package co.immimate.profile.model;

import java.time.Instant;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import co.immimate.user.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a user's immigration profile and application details.
 * Maps directly to the user_immigration_profile_applications table in the database.
 */
@Entity  // Marks this class as a JPA entity (maps to a database table)
@Table(name = "user_immigration_profile_applications")  // Specifies the table name in the database
@Getter  // Generates getter methods for all fields (from Lombok)
@Setter  // Generates setter methods for all fields (from Lombok)
@NoArgsConstructor  // Generates a no-argument constructor (from Lombok)
@AllArgsConstructor  // Generates a constructor with all fields as arguments (from Lombok)
public class UserImmigrationProfile {

    @Id
    @GeneratedValue
    private UUID applicationId;  // Primary Key

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // Foreign Key → Links to User

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String applicantName;

    @Column(nullable = false)
    private int applicantAge;

    @Column(nullable = false)
    private String applicantCitizenship;

    @Column(nullable = false)
    private String applicantResidence;

    @Column(nullable = false)
    private String applicantMaritalStatus;

    @Column(nullable = false)
    private String applicantEducationLevel;
    
    @Column
    private Boolean educationCompletedInCanada;
    
    @Column
    private String canadianEducationLevel;

    @Column(nullable = false)
    private boolean hasEducationalCredentialAssessment;
    
    @Column(nullable = false)
    private String primaryLanguageTestType;
    
    @Column(nullable = false)
    private int primaryTestSpeakingScore;
    
    @Column(nullable = false)
    private int primaryTestListeningScore;
    
    @Column(nullable = false)
    private int primaryTestReadingScore;
    
    @Column(nullable = false)
    private int primaryTestWritingScore;
    
    @Column(nullable = false)
    private boolean tookSecondaryLanguageTest;
    
    @Column
    private String secondaryTestType;
    
    @Column
    private Integer secondaryTestSpeakingScore;
    
    @Column
    private Integer secondaryTestListeningScore;
    
    @Column
    private Integer secondaryTestReadingScore;
    
    @Column
    private Integer secondaryTestWritingScore;
    
    @Column(nullable = false)
    private int canadianWorkExperienceYears;
    
    @Column(nullable = false)
    private Integer nocCodeCanadian;
    
    @Column
    private Integer canadianOccupationTeerCategory;
    
    @Column(nullable = false)
    private int foreignWorkExperienceYears;
    
    @Column
    private Integer nocCodeForeign;
    
    @Column
    private Integer foreignOccupationTeerCategory;
    
    @Column(nullable = false)
    private boolean workingInCanada;
    
    @Column(nullable = false)
    private boolean hasProvincialNomination;
    
    @Column(nullable = false)
    private String provinceOfInterest;
    
    @Column(nullable = false)
    private boolean hasCanadianRelatives;
    
    @Column
    private String relationshipWithCanadianRelative;
    
    @Column(nullable = false)
    private boolean receivedInvitationToApply;
    
    @Column(nullable = false)
    private int settlementFundsCad;
    
    @Column(nullable = false)
    private String preferredCity;
    
    @Column(nullable = false)
    private String preferredDestinationProvince;
    
    @Column
    private String partnerEducationLevel;
    
    @Column
    private String partnerLanguageTestType;
    
    @Column
    private Integer partnerTestSpeakingScore;
    
    @Column
    private Integer partnerTestListeningScore;
    
    @Column
    private Integer partnerTestReadingScore;
    
    @Column
    private Integer partnerTestWritingScore;
    
    @Column
    private Integer partnerCanadianWorkExperienceYears;
    
    @Column
    private Integer spouseOccupationTeerCategory;
    
    @Column(nullable = false)
    private boolean hasJobOffer;
    
    @Column
    private Boolean isJobOfferLmiaApproved;
    
    @Column
    private Integer jobOfferWageCad;
    
    @Column
    private Integer jobOfferNocCode;
    
    @Column
    private Integer jobofferOccupationTeerCategory;
    
    @Column(name = "trades_certification")
    private Boolean tradesCertification;
    
    @Column(nullable = false, columnDefinition = "jsonb")
    @Type(type = "com.vladmihalcea.hibernate.type.json.JsonBinaryType")
    private String jsonPayload;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();  // Auto-set creation time

    @Column(nullable = false)
    private Instant lastModifiedAt = Instant.now();  // Auto-set last update

    @PreUpdate
    public void setLastUpdated() {
        this.lastModifiedAt = Instant.now();
    }
    
    /**
     * Custom toString method to display profile details
     * This bypasses potential Lombok getter issues in test environments
     */
    @Override
    public String toString() {
        return "UserImmigrationProfile{" +
            "applicationId=" + applicationId +
            ", userEmail='" + userEmail + '\'' +
            ", applicantName='" + applicantName + '\'' +
            ", applicantAge=" + applicantAge +
            ", applicantCitizenship='" + applicantCitizenship + '\'' +
            ", applicantResidence='" + applicantResidence + '\'' +
            ", educationLevel='" + applicantEducationLevel + '\'' +
            ", createdAt=" + createdAt +
            ", lastModifiedAt=" + lastModifiedAt +
            '}';
    }
}