package co.immimate.scoring.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.immimate.scoring.model.Evaluation;
import co.immimate.scoring.service.EvaluationScoringService;

/**
 * REST controller for handling immigration profile evaluation requests.
 * Exposes endpoints for triggering evaluations and retrieving results.
 */
@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationScoringService evaluationScoringService;

    public EvaluationController(EvaluationScoringService evaluationScoringService) {
        this.evaluationScoringService = evaluationScoringService;
    }

    /**
     * Endpoint for evaluating a profile using a specific scoring grid.
     * The result includes the total score and details about the evaluation.
     *
     * @param applicationId The ID of the application to evaluate
     * @param gridName The name of the scoring grid to use
     * @return An HTTP response containing the evaluation result
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> evaluateProfile(
            @RequestParam UUID applicationId,
            @RequestParam String gridName
    ) {
        System.out.println("\n[EvaluationController] Received request to evaluate profile: " + 
                          applicationId + " using grid: " + gridName);
        
        try {
            // Perform the evaluation
            Evaluation evaluation = evaluationScoringService.evaluateProfile(applicationId, gridName);
            
            // Create response with evaluation details
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile evaluated successfully");
            response.put("evaluationId", evaluation.getEvaluationId());
            response.put("applicationId", evaluation.getApplicationId());
            response.put("gridName", evaluation.getGridName());
            response.put("totalScore", evaluation.getTotalScore());
            response.put("evaluationDate", evaluation.getEvaluationDate());
            
            System.out.println("[EvaluationController] Evaluation completed with score: " + 
                              evaluation.getTotalScore());
                              
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Handle errors
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error evaluating profile: " + e.getMessage());
            
            System.err.println("[EvaluationController] Error: " + e.getMessage());
            e.printStackTrace();
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
