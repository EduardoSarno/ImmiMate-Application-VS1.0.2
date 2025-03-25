package co.immimate.scoringevaluations.evaluation.model;

import java.time.Instant;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a field evaluation within a subcategory evaluation.
 * Maps to the "evaluation_fields" table in the user_immigration_evaluation schema.
 */
@Entity
@Table(name = "evaluation_fields", schema = "user_immigration_evaluation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationField {

    @Id
    @Column(name = "field_eval_id")
    private UUID fieldEvalId;
    
    @Column(name = "subcat_eval_id", nullable = false)
    private UUID subcatEvalId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcat_eval_id", referencedColumnName = "subcat_eval_id", insertable = false, updatable = false)
    private EvaluationSubcategory subcategory;
    
    @Column(name = "field_id", nullable = false)
    private UUID fieldId;
    
    @Column(name = "application_id", nullable = false)
    private UUID applicationId;
    
    @Column(name = "user_points_earned")
    private Integer userPointsEarned = 0;
    
    @Column(name = "user_qualifies")
    private Boolean userQualifies = false;
    
    @Column(name = "logic_expression")
    private String logicExpression;
    
    @Column(name = "actual_value")
    private String actualValue;
    
    @Column(name = "field_name")
    private String fieldName;
    
    @Column(name = "evaluation_date")
    private Instant evaluationDate;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    /**
     * Constructor with essential fields
     */
    public EvaluationField(UUID fieldEvalId, UUID subcatEvalId, UUID fieldId, UUID applicationId,
                          Integer userPointsEarned, Boolean userQualifies, String logicExpression,
                          String actualValue, String fieldName) {
        this.fieldEvalId = fieldEvalId;
        this.subcatEvalId = subcatEvalId;
        this.fieldId = fieldId;
        this.applicationId = applicationId;
        this.userPointsEarned = userPointsEarned;
        this.userQualifies = userQualifies;
        this.logicExpression = logicExpression;
        this.actualValue = actualValue;
        this.fieldName = fieldName;
        this.evaluationDate = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
} 