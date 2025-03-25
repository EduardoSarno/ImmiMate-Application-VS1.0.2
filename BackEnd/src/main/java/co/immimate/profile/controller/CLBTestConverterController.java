package co.immimate.profile.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.immimate.profile.dto.CLBConversionResponse;
import co.immimate.profile.service.CLBTestConverterService;

/**
 * Controller for CLB test conversion endpoints
 */
@RestController
@RequestMapping(CLBTestConverterController.BASE_PATH)
public class CLBTestConverterController {
    // API path constants
    public static final String BASE_PATH = "/api/language-tests";
    private static final String CONVERSIONS_PATH = "/conversions";
    private static final String CONVERT_PATH = "/convert";
    private static final String OPTIONS_PATH = "/options";
    
    // Request parameters
    private static final String PARAM_TEST_TYPE = "testType";
    private static final String PARAM_SKILL = "skill";
    private static final String PARAM_SCORE = "score";
    
    // Log messages
    private static final String LOG_REQUEST_TABLES = "Request received for CLB test conversion tables";
    private static final String LOG_REQUEST_CONVERT = "Request to convert score: testType={}, skill={}, score={}";
    
    private static final Logger logger = LoggerFactory.getLogger(CLBTestConverterController.class);
    
    @Autowired
    private CLBTestConverterService service;
    
    /**
     * Get all CLB test conversion data
     * @return CLB conversion tables for all test types
     */
    @GetMapping(CONVERSIONS_PATH)
    public ResponseEntity<CLBConversionResponse> getConversionTables() {
        logger.info(LOG_REQUEST_TABLES);
        return ResponseEntity.ok(service.getConversionTables());
    }
    
    /**
     * Convert a specific test score to CLB level
     * @param testType The language test type (CELPIP, IELTS, etc.)
     * @param skill The language skill (listening, reading, etc.)
     * @param score The score value
     * @return The CLB level or 0 if not found
     */
    @GetMapping(CONVERT_PATH)
    public ResponseEntity<Integer> convertToCLB(
            @RequestParam(PARAM_TEST_TYPE) String testType,
            @RequestParam(PARAM_SKILL) String skill,
            @RequestParam(PARAM_SCORE) String score) {
        
        logger.info(LOG_REQUEST_CONVERT, testType, skill, score);
        Integer clbLevel = service.convertToCLB(testType, skill, score);
        
        if (clbLevel == null) {
            // Return 0 instead of 404 for scores that couldn't be converted
            logger.warn("No CLB level found for test={}, skill={}, score={}, returning 0", testType, skill, score);
            return ResponseEntity.ok(0);
        }
        
        return ResponseEntity.ok(clbLevel);
    }

    /**
     * Get all possible score options for each test type and skill
     * This is used by the frontend to populate dropdown menus
     * @return Map of test types to skills to valid score values
     */
    @GetMapping(OPTIONS_PATH)
    public ResponseEntity<Map<String, Map<String, List<String>>>> getScoreOptions() {
        logger.info("Request received for language test score options");
        return ResponseEntity.ok(service.getScoreOptions());
    }
} 