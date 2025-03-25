package co.immimate.scoringevaluations.grid.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.immimate.scoringevaluations.grid.model.Grid;

/**
 * Repository for accessing Grid entities from the database.
 */
@Repository
public interface GridRepository extends JpaRepository<Grid, UUID> {
    
    /**
     * Find a grid by its grid name.
     * 
     * @param gridName The name of the grid to find
     * @return Optional grid
     */
    Optional<Grid> findByGridName(String gridName);
    
    /**
     * Get the maximum points possible for a category with spouse.
     * 
     * @param gridName The name of the grid
     * @param categoryName The name of the category
     * @return The maximum points possible
     */
    @Query(value = "SELECT max_points_spouse FROM static_canadian_immigration_data.grids_categories " +
                  "WHERE grid_name = :gridName AND category_name = :categoryName", nativeQuery = true)
    Integer getMaxPointsForCategoryWithSpouse(@Param("gridName") String gridName, 
                                            @Param("categoryName") String categoryName);
    
    /**
     * Get the maximum points possible for a category without spouse.
     * 
     * @param gridName The name of the grid
     * @param categoryName The name of the category
     * @return The maximum points possible
     */
    @Query(value = "SELECT max_points_no_spouse FROM static_canadian_immigration_data.grids_categories " +
                  "WHERE grid_name = :gridName AND category_name = :categoryName", nativeQuery = true)
    Integer getMaxPointsForCategoryWithoutSpouse(@Param("gridName") String gridName, 
                                              @Param("categoryName") String categoryName);
    
    /**
     * Get the maximum points possible for a subcategory with spouse.
     * 
     * @param gridName The name of the grid
     * @param subcategoryName The name of the subcategory
     * @return The maximum points possible
     */
    @Query(value = "SELECT max_points_spouse FROM static_canadian_immigration_data.grids_subcategories " +
                  "WHERE grid_name = :gridName AND subcategory_name = :subcategoryName", nativeQuery = true)
    Integer getMaxPointsForSubcategoryWithSpouse(@Param("gridName") String gridName, 
                                              @Param("subcategoryName") String subcategoryName);
    
    /**
     * Get the maximum points possible for a subcategory without spouse.
     * 
     * @param gridName The name of the grid
     * @param subcategoryName The name of the subcategory
     * @return The maximum points possible
     */
    @Query(value = "SELECT max_points_no_spouse FROM static_canadian_immigration_data.grids_subcategories " +
                  "WHERE grid_name = :gridName AND subcategory_name = :subcategoryName", nativeQuery = true)
    Integer getMaxPointsForSubcategoryWithoutSpouse(@Param("gridName") String gridName, 
                                                @Param("subcategoryName") String subcategoryName);
} 