package co.immimate.scoringevaluations.grid.model;

import java.time.Instant;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a subcategory within a category in a scoring grid.
 * Maps to the "grids_subcategories" table in the static_canadian_immigration_data schema.
 */
@Entity
@Table(name = "grids_subcategories", schema = "static_canadian_immigration_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GridSubcategory {

    @Id
    @Column(name = "subcategory_id")
    private UUID subcategoryId;
    
    @Column(name = "category_id", nullable = false)
    private UUID categoryId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", referencedColumnName = "category_id", insertable = false, updatable = false)
    private GridCategory category;
    
    @Column(name = "subcategory_name", nullable = false)
    private String subcategoryName;
    
    @Column(name = "subcategory_description")
    private String subcategoryDescription;
    
    @Column(name = "max_points_spouse")
    private Integer maxPointsSpouse;
    
    @Column(name = "max_points_no_spouse")
    private Integer maxPointsNoSpouse;
    
    @Column(name = "sort_order")
    private Integer sortOrder;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @Column(name = "updated_by", nullable = false)
    private String updatedBy;
    
    @Column(name = "category_name")
    private String categoryName;
    
    @Column(name = "grid_name")
    private String gridName;
} 