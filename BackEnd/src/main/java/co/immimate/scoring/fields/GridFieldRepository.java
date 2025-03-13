package co.immimate.scoring.fields;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for accessing scoring grid fields from the database.
 */
@Repository
public interface GridFieldRepository extends JpaRepository<GridField, UUID> {

    /**
     * Retrieve all grid fields for the specified grid name.
     *
     * @param gridName The name of the scoring grid (e.g., "CRS").
     * @return A list of GridField objects that belong to the given grid.
     */
    List<GridField> findAllByGridName(String gridName);
}
