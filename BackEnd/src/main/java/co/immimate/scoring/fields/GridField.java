package co.immimate.scoring.fields;

import java.time.Instant;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a single scoring rule field from the static scoring database.
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
    private UUID fieldId;  // Unique identifier for the field

    @Column(name = "field_name", nullable = false)
    private String fieldName; // Name of the scoring field

    @Column(name = "points_with_spouse", nullable = false)
    private int pointsWithSpouse;  // Points if the user is married

    @Column(name = "points_without_spouse", nullable = false)
    private int pointsWithoutSpouse;  // Points if the user is single

    @Column(name = "logic_operator")
    private String logicOperator;  // Operator used in the logic (e.g., "AND", "NONE", "OR")

    @Column(name = "logic_expression", nullable = false)
    private String logicExpression;  // Expression to evaluate (e.g., "applicant_age>=20; applicant_age<=29")

    @Column(name = "notes")
    private String notes;  // Additional notes about the field

    @Column(name = "sort_order")
    private int sortOrder;  // Order in which fields are processed

    @Column(name = "subcategory_name")
    private String subcategoryName;  // Name of the subcategory (e.g., "Age")

    @Column(name = "category_name")
    private String categoryName;  // Name of the category (e.g., "A. Core / Human Capital Factors")

    @Column(name = "grid_name")
    private String gridName;  // Name of the grid (e.g., "Comprehensive Ranking System (CRS)")

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;  // Timestamp for creation

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;  // Timestamp for last update

    @Column(name = "updated_by")
    private String updatedBy;  // Who last updated this field
}
