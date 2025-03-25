package co.immimate.scoringevaluations.grid.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.immimate.scoringevaluations.grid.model.GridField;

/**
 * Repository for accessing GridField entities from the database.
 */
@Repository
public interface GridFieldRepository extends JpaRepository<GridField, UUID> {
    
    /**
     * Find all fields for a specific subcategory ID.
     * 
     * @param subcategoryId The ID of the subcategory
     * @return List of grid fields
     */
    List<GridField> findBySubcategoryId(UUID subcategoryId);
    
    /**
     * Find all fields for a specific subcategory name in a specific category and grid.
     * 
     * @param subcategoryName The name of the subcategory
     * @param categoryName The name of the category
     * @param gridName The name of the grid
     * @return List of grid fields
     */
    List<GridField> findBySubcategoryNameAndCategoryNameAndGridName(
            String subcategoryName, String categoryName, String gridName);
} 