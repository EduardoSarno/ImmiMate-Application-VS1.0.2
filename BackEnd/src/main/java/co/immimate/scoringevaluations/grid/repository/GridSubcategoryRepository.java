package co.immimate.scoringevaluations.grid.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.immimate.scoringevaluations.grid.model.GridSubcategory;

/**
 * Repository for accessing GridSubcategory entities from the database.
 */
@Repository
public interface GridSubcategoryRepository extends JpaRepository<GridSubcategory, UUID> {
    
    /**
     * Find all subcategories for a specific category ID.
     * 
     * @param categoryId The ID of the category
     * @return List of grid subcategories
     */
    List<GridSubcategory> findByCategoryId(UUID categoryId);
    
    /**
     * Find all subcategories for a specific category name in a specific grid.
     * 
     * @param categoryName The name of the category
     * @param gridName The name of the grid
     * @return List of grid subcategories
     */
    List<GridSubcategory> findByCategoryNameAndGridName(String categoryName, String gridName);
} 