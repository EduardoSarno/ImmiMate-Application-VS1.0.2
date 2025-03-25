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
 * Represents a field within a subcategory in a scoring grid.
 * Maps to the "grids_fields" table in the static_canadian_immigration_data schema.
 */
@Entity
@Table(name = "grids_fields", schema = "static_canadian_immigration_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GridField {

    @Id
    @Column(name = "field_id")
    private UUID fieldId;
    
    @Column(name = "subcategory_id", nullable = false)
    private UUID subcategoryId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id", referencedColumnName = "subcategory_id", insertable = false, updatable = false)
    private GridSubcategory subcategory;
    
    @Column(name = "field_name", nullable = false)
    private String fieldName;
    
    @Column(name = "field_description")
    private String fieldDescription;
    
    @Column(name = "logic_expression")
    private String logicExpression;
    
    @Column(name = "logic_operator")
    private String logicOperator;
    
    @Column(name = "points_without_spouse")
    private Integer pointsWithoutSpouse;
    
    @Column(name = "points_with_spouse")
    private Integer pointsWithSpouse;
    
    @Column(name = "mutually_exclusive")
    private Boolean mutuallyExclusive;
    
    @Column(name = "sort_order")
    private Integer sortOrder;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @Column(name = "updated_by", nullable = false)
    private String updatedBy;
    
    @Column(name = "subcategory_name")
    private String subcategoryName;
    
    @Column(name = "category_id")
    private UUID categoryId;
    
    @Column(name = "category_name")
    private String categoryName;
    
    @Column(name = "grid_id")
    private UUID gridId;
    
    @Column(name = "grid_name")
    private String gridName;
} 