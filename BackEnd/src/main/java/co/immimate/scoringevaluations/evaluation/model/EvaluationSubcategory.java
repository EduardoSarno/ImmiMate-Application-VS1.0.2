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
 * Represents a subcategory evaluation within a category evaluation.
 * Maps to the "evaluation_subcategories" table in the user_immigration_evaluation schema.
 */
@Entity
@Table(name = "evaluation_subcategories", schema = "user_immigration_evaluation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationSubcategory {

    @Id
    @Column(name = "subcat_eval_id")
    private UUID subcatEvalId;
    
    @Column(name = "cat_eval_id", nullable = false)
    private UUID catEvalId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cat_eval_id", referencedColumnName = "cat_eval_id", insertable = false, updatable = false)
    private EvaluationCategory category;
    
    @Column(name = "subcategory_id", nullable = false)
    private UUID subcategoryId;
    
    @Column(name = "subcategory_name")
    private String subcategoryName;
    
    @Column(name = "user_score")
    private Integer userScore = 0;
    
    @Column(name = "max_possible_score")
    private Integer maxPossibleScore = 0;
    
    @Column(name = "field_count")
    private Integer fieldCount = 0;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
} 