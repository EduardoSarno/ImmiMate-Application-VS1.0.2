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
 * Represents a category evaluation within an overall evaluation.
 * Maps to the "evaluation_categories" table in the user_immigration_evaluation schema.
 */
@Entity
@Table(name = "evaluation_categories", schema = "user_immigration_evaluation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationCategory {

    @Id
    @Column(name = "cat_eval_id")
    private UUID catEvalId;
    
    @Column(name = "evaluation_id", nullable = false)
    private UUID evaluationId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", referencedColumnName = "evaluation_id", insertable = false, updatable = false)
    private Evaluation evaluation;
    
    @Column(name = "category_id", nullable = false)
    private UUID categoryId;
    
    @Column(name = "category_name")
    private String categoryName;
    
    @Column(name = "user_score")
    private Integer userScore = 0;
    
    @Column(name = "max_possible_score")
    private Integer maxPossibleScore = 0;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
} 