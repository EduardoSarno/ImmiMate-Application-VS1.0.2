package co.immimate.scoring.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.immimate.scoring.model.Evaluation;

/**
 * Repository for storing and retrieving evaluation results.
 * Connects to the actual database.
 */
@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

    /**
     * Retrieves the most recent evaluation for a specific application ID and grid name.
     * Spring Data JPA automatically implements this query.
     *
     * @param applicationId The application ID to search for
     * @param gridName The grid name to search for
     * @return The most recent evaluation, or null if none found
     */
    Evaluation findTopByApplicationIdAndGridNameOrderByEvaluationDateDesc(UUID applicationId, String gridName);
}