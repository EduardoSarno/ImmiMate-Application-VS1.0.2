package co.immimate.scoringevaluations.evaluation.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.immimate.scoringevaluations.evaluation.model.EvaluationField;

/**
 * Repository for accessing EvaluationField entities from the database.
 */
@Repository
public interface EvaluationFieldRepository extends JpaRepository<EvaluationField, UUID> {
    
    /**
     * Find all evaluation fields for a specific subcategory evaluation ID.
     * 
     * @param subcatEvalId The ID of the subcategory evaluation
     * @return List of evaluation fields
     */
    List<EvaluationField> findBySubcatEvalId(UUID subcatEvalId);
    
    /**
     * Find all evaluation fields for a specific application ID.
     * 
     * @param applicationId The ID of the application
     * @return List of evaluation fields
     */
    List<EvaluationField> findByApplicationId(UUID applicationId);
    
    /**
     * Find a specific evaluation field by subcategory evaluation ID and field name.
     * 
     * @param subcatEvalId The ID of the subcategory evaluation
     * @param fieldName The name of the field
     * @return List of evaluation fields
     */
    List<EvaluationField> findBySubcatEvalIdAndFieldName(UUID subcatEvalId, String fieldName);
} 