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
 * Represents a category within a scoring grid.
 * Maps to the "grids_categories" table in the static_canadian_immigration_data schema.
 */
@Entity
@Table(name = "grids_categories", schema = "static_canadian_immigration_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GridCategory {

    @Id
    @Column(name = "category_id")
    private UUID categoryId;
    
    @Column(name = "grid_id", nullable = false)
    private UUID gridId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grid_id", referencedColumnName = "grid_id", insertable = false, updatable = false)
    private Grid grid;
    
    @Column(name = "category_name", nullable = false)
    private String categoryName;
    
    @Column(name = "category_description")
    private String categoryDescription;
    
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
    
    @Column(name = "grid_name")
    private String gridName;
} 