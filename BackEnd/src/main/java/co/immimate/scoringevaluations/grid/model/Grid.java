package co.immimate.scoringevaluations.grid.model;

import java.time.Instant;
import java.time.LocalDate;
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
 * Represents a scoring grid from the static scoring database.
 * Maps to the "grids" table in the static_canadian_immigration_data schema.
 */
@Entity
@Table(name = "grids", schema = "static_canadian_immigration_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Grid {

    @Id
    @Column(name = "grid_id")
    private UUID gridId;
    
    @Column(name = "grid_name", nullable = false)
    private String gridName;
    
    @Column(name = "grid_version")
    private String gridVersion;
    
    @Column(name = "coverage")
    private String coverage;
    
    @Column(name = "max_total_points")
    private Integer maxTotalPoints;
    
    @Column(name = "effective_date")
    private LocalDate effectiveDate;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @Column(name = "updated_by", nullable = false)
    private String updatedBy;
} 