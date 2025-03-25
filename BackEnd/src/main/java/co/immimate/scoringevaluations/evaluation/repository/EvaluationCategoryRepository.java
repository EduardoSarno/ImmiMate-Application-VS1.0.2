package co.immimate.scoringevaluations.evaluation.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.immimate.scoringevaluations.evaluation.model.EvaluationCategory;

/**
 * Repository for accessing EvaluationCategory entities from the database.
 */
@Repository
public interface EvaluationCategoryRepository extends JpaRepository<EvaluationCategory, UUID> {
    
    /**
     * Find all evaluation categories for a specific evaluation ID.
     * 
     * @param evaluationId The ID of the evaluation
     * @return List of evaluation categories
     */
    List<EvaluationCategory> findByEvaluationId(UUID evaluationId);
    
    /**
     * Find a specific evaluation category by evaluation ID and category name.
     * 
     * @param evaluationId The ID of the evaluation
     * @param categoryName The name of the category
     * @return List of evaluation categories
     */
    List<EvaluationCategory> findByEvaluationIdAndCategoryName(UUID evaluationId, String categoryName);
} 