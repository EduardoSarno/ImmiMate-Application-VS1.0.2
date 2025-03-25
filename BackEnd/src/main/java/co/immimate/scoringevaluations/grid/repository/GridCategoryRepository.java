package co.immimate.scoringevaluations.grid.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.immimate.scoringevaluations.grid.model.GridCategory;

/**
 * Repository for accessing GridCategory entities from the database.
 */
@Repository
public interface GridCategoryRepository extends JpaRepository<GridCategory, UUID> {
    
    /**
     * Find all categories for a specific grid ID.
     * 
     * @param gridId The ID of the grid
     * @return List of grid categories
     */
    List<GridCategory> findByGridId(UUID gridId);
    
    /**
     * Find all categories for a specific grid name.
     * 
     * @param gridName The name of the grid
     * @return List of grid categories
     */
    List<GridCategory> findByGridName(String gridName);
} 