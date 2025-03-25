package co.immimate.scoringevaluations.evaluation.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.immimate.scoringevaluations.evaluation.model.Evaluation;

/**
 * Repository for accessing Evaluation entities from the database.
 */
@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {
    
    /**
     * Find all evaluations for a specific application ID.
     * 
     * @param applicationId The ID of the application
     * @return List of evaluations
     */
    List<Evaluation> findByApplicationId(UUID applicationId);
    
    /**
     * Find the latest evaluation for a specific application ID.
     * 
     * @param applicationId The ID of the application
     * @return Optional evaluation
     */
    @Query(value = "SELECT * FROM user_immigration_evaluation.evaluations " +
                 "WHERE application_id = :applicationId " +
                 "ORDER BY evaluation_date DESC LIMIT 1", nativeQuery = true)
    Optional<Evaluation> findLatestByApplicationId(@Param("applicationId") UUID applicationId);
    
    /**
     * Find the latest evaluation for a specific application ID and grid name.
     * 
     * @param applicationId The ID of the application
     * @param gridName The name of the grid
     * @return Optional evaluation
     */
    @Query(value = "SELECT * FROM user_immigration_evaluation.evaluations " +
                 "WHERE application_id = :applicationId AND grid_name = :gridName " +
                 "ORDER BY evaluation_date DESC LIMIT 1", nativeQuery = true)
    Optional<Evaluation> findLatestByApplicationIdAndGridName(
            @Param("applicationId") UUID applicationId, @Param("gridName") String gridName);
} 