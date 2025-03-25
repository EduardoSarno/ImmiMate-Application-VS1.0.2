package co.immimate.scoringevaluations.evaluation.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.immimate.scoringevaluations.evaluation.model.EvaluationSubcategory;

/**
 * Repository for accessing EvaluationSubcategory entities from the database.
 */
@Repository
public interface EvaluationSubcategoryRepository extends JpaRepository<EvaluationSubcategory, UUID> {
    
    /**
     * Find all evaluation subcategories for a specific category evaluation ID.
     * 
     * @param catEvalId The ID of the category evaluation
     * @return List of evaluation subcategories
     */
    List<EvaluationSubcategory> findByCatEvalId(UUID catEvalId);
    
    /**
     * Find a specific evaluation subcategory by category evaluation ID and subcategory name.
     * 
     * @param catEvalId The ID of the category evaluation
     * @param subcategoryName The name of the subcategory
     * @return List of evaluation subcategories
     */
    List<EvaluationSubcategory> findByCatEvalIdAndSubcategoryName(UUID catEvalId, String subcategoryName);
} 