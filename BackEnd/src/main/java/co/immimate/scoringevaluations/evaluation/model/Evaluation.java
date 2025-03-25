package co.immimate.scoringevaluations.evaluation.model;

import java.time.Instant;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the result of an immigration profile evaluation.
 * Maps to the "evaluations" table in the user_immigration_evaluation schema.
 */
@Entity
@Table(name = "evaluations", schema = "user_immigration_evaluation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Evaluation {

    @Id
    @Column(name = "evaluation_id")
    private UUID evaluationId;
    
    @Column(name = "application_id", nullable = false)
    private UUID applicationId;
    
    @Column(name = "grid_id", nullable = false)
    private UUID gridId;
    
    @Column(name = "grid_name", nullable = false)
    private String gridName;
    
    @Column(name = "evaluation_date", nullable = false)
    private Instant evaluationDate;
    
    @Column(name = "total_score")
    private Integer totalScore;
    
    @Column(name = "notes")
    private String notes;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @Column(name = "evaluation_details")
    private String evaluationDetails;
    
    @Column(name = "status", columnDefinition = "VARCHAR(50) DEFAULT 'COMPLETED'")
    private String status = "COMPLETED";
    
    @Column(name = "version", columnDefinition = "INTEGER DEFAULT 1")
    private Integer version = 1;
    
    /**
     * Check if this evaluation is valid.
     * @return true if the evaluation is COMPLETED, false otherwise
     */
    public boolean isValid() {
        return "COMPLETED".equals(status);
    }
} 