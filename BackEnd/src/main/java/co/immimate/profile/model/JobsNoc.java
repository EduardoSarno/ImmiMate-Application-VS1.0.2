package co.immimate.profile.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing the NOC (National Occupational Classification) codes and their TEER categories.
 * Maps to the static_canadian_immigration_data.jobs_noc table in the database.
 */
@Entity
@Table(name = "jobs_noc", schema = "static_canadian_immigration_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobsNoc {
    
    @Id
    @Column(name = "noc_code")
    private Integer nocCode;
    
    @Column(name = "teer_category")
    private Integer teerCategory;
    
    @Column(name = "job_title")
    private String jobTitle;
    
    @Column(name = "job_category")
    private String jobCategory;
    
    @Override
    public String toString() {
        return "JobsNoc{" +
               "nocCode=" + nocCode +
               ", teerCategory=" + teerCategory +
               ", jobTitle='" + jobTitle + '\'' +
               ", jobCategory='" + jobCategory + '\'' +
               '}';
    }
} 