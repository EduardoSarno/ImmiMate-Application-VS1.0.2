package co.immimate.scoringevaluations.calculation.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.immimate.scoringevaluations.evaluation.model.Evaluation;
import co.immimate.scoringevaluations.evaluation.repository.EvaluationRepository;

/**
 * Controller for managing evaluations.
 * 
 * This controller works with the EvaluationService to create and retrieve evaluations.
 * The workflow is as follows:
 * 1. When creating an evaluation, it gets user variables from the EvaluationService
 * 2. It checks if the applicant has a spouse using the EvaluationService
 * 3. It creates an evaluation using the EvaluationService's createEvaluation method
 * 
 * The EvaluationService is responsible for all the business logic related to evaluations,
 * while this controller handles the HTTP requests and responses.
 */
@RestController
@RequestMapping(EvaluationController.BASE_PATH)
public class EvaluationController {
    // API paths
    public static final String BASE_PATH = "/api/evaluations";
    private static final String CREATE_PATH = "/create";
    private static final String APPLICATION_PATH = "/application/{applicationId}";
    private static final String LATEST_PATH = "/application/{applicationId}/latest";
    private static final String EVALUATION_ID_PATH = "/{evaluationId}";
    
    // Request parameters
    private static final String PARAM_APPLICATION_ID = "applicationId";
    private static final String PARAM_GRID_NAME = "gridName";
    
    // Path variables
    private static final String PATH_VAR_APPLICATION_ID = "applicationId";
    private static final String PATH_VAR_EVALUATION_ID = "evaluationId";
    
    // Log messages
    private static final String LOG_CREATING_EVALUATION = "Creating evaluation for application {} using grid {}";
    private static final String LOG_GETTING_EVALUATIONS = "Getting evaluations for application {}";
    private static final String LOG_GETTING_LATEST = "Getting latest evaluation for application {}";
    private static final String LOG_GETTING_BY_ID = "Getting evaluation with ID {}";

    private static final Logger logger = LoggerFactory.getLogger(EvaluationController.class);

    @Autowired
    private EvaluationService evaluationService;
    
    @Autowired
    private EvaluationRepository evaluationRepository;
    
    /**
     * Create a new evaluation for an application.
     * 
     * This endpoint demonstrates how the EvaluationService is used:
     * 1. Get user variables from the service
     * 2. Check if the applicant has a spouse
     * 3. Create the evaluation using the service
     * 
     * @param applicationId The ID of the application to evaluate
     * @param gridName The name of the grid to use for evaluation
     * @return The created evaluation
     */
    @PostMapping(CREATE_PATH)
    public ResponseEntity<Evaluation> createEvaluation(
            @RequestParam(PARAM_APPLICATION_ID) UUID applicationId,
            @RequestParam(PARAM_GRID_NAME) String gridName) {
        
        logger.info(LOG_CREATING_EVALUATION, applicationId, gridName);
        
        // Get user variables
        Map<String, Object> userVariables = evaluationService.getUserVariables(applicationId);
        
        // Check if the applicant has a spouse
        boolean hasSpouse = evaluationService.hasSpouse(applicationId);
        
        // Create the evaluation
        Evaluation evaluation = evaluationService.createEvaluation(
                applicationId, gridName, userVariables, hasSpouse);
        
        return ResponseEntity.ok(evaluation);
    }
    
    /**
     * Get all evaluations for an application.
     * 
     * @param applicationId The ID of the application
     * @return List of evaluations
     */
    @GetMapping(APPLICATION_PATH)
    public ResponseEntity<List<Evaluation>> getEvaluationsByApplication(
            @PathVariable(PATH_VAR_APPLICATION_ID) UUID applicationId) {
        
        logger.info(LOG_GETTING_EVALUATIONS, applicationId);
        
        List<Evaluation> evaluations = evaluationRepository.findByApplicationId(applicationId);
        
        return ResponseEntity.ok(evaluations);
    }
    
    /**
     * Get the latest evaluation for an application.
     * 
     * @param applicationId The ID of the application
     * @return The latest evaluation
     */
    @GetMapping(LATEST_PATH)
    public ResponseEntity<Evaluation> getLatestEvaluationByApplication(
            @PathVariable(PATH_VAR_APPLICATION_ID) UUID applicationId) {
        
        logger.info(LOG_GETTING_LATEST, applicationId);
        
        return evaluationRepository.findLatestByApplicationId(applicationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get a specific evaluation by ID.
     * 
     * @param evaluationId The ID of the evaluation
     * @return The evaluation
     */
    @GetMapping(EVALUATION_ID_PATH)
    public ResponseEntity<Evaluation> getEvaluationById(
            @PathVariable(PATH_VAR_EVALUATION_ID) UUID evaluationId) {
        
        logger.info(LOG_GETTING_BY_ID, evaluationId);
        
        return evaluationRepository.findById(evaluationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}