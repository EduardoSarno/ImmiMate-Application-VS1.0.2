package co.immimate.scoring.model;

import java.time.Instant;
import java.util.UUID;

import javax.persistence.Column;  // <-- Import JPA annotations
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the result of an immigration profile evaluation.
 * Contains the score and metadata about the evaluation.
 */
@Entity  // <-- Marks this as a JPA entity
@Table(name = "evaluations", schema = "user_immigration_evaluation") // <-- Ensure table name matches your database
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Evaluation {

    @Id  // <-- PRIMARY KEY
    @GeneratedValue(strategy = GenerationType.AUTO) // <-- Auto-generate ID
    private UUID evaluationId;    

    @Column(nullable = false) // Ensure this is not null in DB
    private UUID applicationId;     

    @Column(nullable = false)
    private String gridName;        

    private int totalScore;         

    @Column(nullable = false, updatable = false)
    private Instant evaluationDate; 

    private String evaluationDetails; 

    /**
     * Constructor without evaluationDetails
     */
    public Evaluation(UUID evaluationId, UUID applicationId, String gridName, int totalScore, Instant evaluationDate) {
        this.evaluationId = evaluationId;
        this.applicationId = applicationId;
        this.gridName = gridName;
        this.totalScore = totalScore;
        this.evaluationDate = evaluationDate;
        this.evaluationDetails = "";
    }
    
    @Override
    public String toString() {
        return """
               Evaluation 
               {
                 evaluationId: """ + evaluationId + ",\n" +
               "  applicationId: " + applicationId + ",\n" +
               "  gridName: " + gridName + ",\n" +
               "  totalScore: " + totalScore + ",\n" +
               "  evaluationDate: " + evaluationDate + ",\n" +
               (evaluationDetails != null && !evaluationDetails.isEmpty() ? "  evaluationDetails: " + evaluationDetails + "\n" : "") +
               "}";
    }
}