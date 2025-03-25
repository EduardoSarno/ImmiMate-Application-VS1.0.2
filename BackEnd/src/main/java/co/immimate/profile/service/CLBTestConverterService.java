package co.immimate.profile.service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import co.immimate.profile.dto.CLBConversionResponse;
import co.immimate.profile.model.CLBTestConverter;
import co.immimate.profile.repository.CLBTestConverterRepository;

/**
 * Service for handling CLB test conversion operations
 */
@Service
public class CLBTestConverterService {
    // Logging and formatting
    private static final Logger logger = LoggerFactory.getLogger(CLBTestConverterService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Cache names
    private static final String CACHE_CLB_CONVERSIONS = "clbConversions";
    
    // Log messages
    private static final String LOG_FETCHING_TABLES = "Fetching CLB conversion tables from database";
    private static final String LOG_PARSING_ERROR = "Error parsing score as number: {}";
    
    // Test types
    private static final String TEST_CELPIP = "CELPIP";
    private static final String TEST_IELTS = "IELTS";
    private static final String TEST_PTE = "PTE";
    private static final String TEST_TEF = "TEF";
    private static final String TEST_TCF = "TCF";
    
    // Language skills
    private static final String SKILL_LISTENING = "listening";
    private static final String SKILL_READING = "reading";
    private static final String SKILL_WRITING = "writing";
    private static final String SKILL_SPEAKING = "speaking";
    
    // Response metadata
    private static final String METADATA_UNKNOWN = "Unknown";
    
    // Special values
    private static final String NOT_APPLICABLE = "N/A";
    private static final String RANGE_SEPARATOR = "-";
    
    @Autowired
    private CLBTestConverterRepository repository;
    
    /**
     * Get all CLB test conversions
     * @return List of CLB test converter entities
     */
    public List<CLBTestConverter> getAllConversions() {
        return repository.findByActiveIsTrueOrderByClbLevelDesc();
    }
    
    /**
     * Get all CLB test conversions formatted for client use
     * Cached to improve performance
     * @return CLBConversionResponse with structured test conversion data
     */
    @Cacheable(CACHE_CLB_CONVERSIONS)
    public CLBConversionResponse getConversionTables() {
        logger.info(LOG_FETCHING_TABLES);
        List<CLBTestConverter> conversions = getAllConversions();
        CLBConversionResponse response = new CLBConversionResponse();
        
        // Initialize maps for each test type
        Map<String, Map<String, Object>> celpip = new HashMap<>();
        Map<String, Map<String, Object>> ielts = new HashMap<>();
        Map<String, Map<String, Object>> pte = new HashMap<>();
        Map<String, Map<String, Object>> tef = new HashMap<>();
        Map<String, Map<String, Object>> tcf = new HashMap<>();
        
        // Initialize skill maps for each test type
        initializeSkillMaps(celpip);
        initializeSkillMaps(ielts);
        initializeSkillMaps(pte);
        initializeSkillMaps(tef);
        initializeSkillMaps(tcf);
        
        // Populate maps with conversion data
        for (CLBTestConverter conversion : conversions) {
            // Process CELPIP scores
            processScores(celpip, SKILL_LISTENING, conversion.getCelpipListening(), conversion.getClbLevel());
            processScores(celpip, SKILL_READING, conversion.getCelpipReading(), conversion.getClbLevel());
            processScores(celpip, SKILL_WRITING, conversion.getCelpipWriting(), conversion.getClbLevel());
            processScores(celpip, SKILL_SPEAKING, conversion.getCelpipSpeaking(), conversion.getClbLevel());
            
            // Process IELTS scores
            processScores(ielts, SKILL_LISTENING, conversion.getIeltsListening(), conversion.getClbLevel());
            processScores(ielts, SKILL_READING, conversion.getIeltsReading(), conversion.getClbLevel());
            processScores(ielts, SKILL_WRITING, conversion.getIeltsWriting(), conversion.getClbLevel());
            processScores(ielts, SKILL_SPEAKING, conversion.getIeltsSpeaking(), conversion.getClbLevel());
            
            // Process PTE scores
            processScores(pte, SKILL_LISTENING, conversion.getPteListening(), conversion.getClbLevel());
            processScores(pte, SKILL_READING, conversion.getPteReading(), conversion.getClbLevel());
            processScores(pte, SKILL_WRITING, conversion.getPteWriting(), conversion.getClbLevel());
            processScores(pte, SKILL_SPEAKING, conversion.getPteSpeaking(), conversion.getClbLevel());
            
            // Process TEF scores
            processScores(tef, SKILL_LISTENING, conversion.getTefListening(), conversion.getClbLevel());
            processScores(tef, SKILL_READING, conversion.getTefReading(), conversion.getClbLevel());
            processScores(tef, SKILL_WRITING, conversion.getTefWriting(), conversion.getClbLevel());
            processScores(tef, SKILL_SPEAKING, conversion.getTefSpeaking(), conversion.getClbLevel());
            
            // Process TCF scores
            processScores(tcf, SKILL_LISTENING, conversion.getTcfListening(), conversion.getClbLevel());
            processScores(tcf, SKILL_READING, conversion.getTcfReading(), conversion.getClbLevel());
            processScores(tcf, SKILL_WRITING, conversion.getTcfWriting(), conversion.getClbLevel());
            processScores(tcf, SKILL_SPEAKING, conversion.getTcfSpeaking(), conversion.getClbLevel());
        }
        
        // Set response fields
        response.setCelpip(celpip);
        response.setIelts(ielts);
        response.setPte(pte);
        response.setTef(tef);
        response.setTcf(tcf);
        
        // Set metadata
        if (!conversions.isEmpty()) {
            response.setLastUpdated(
                conversions.get(0).getLastUpdated() != null ? 
                conversions.get(0).getLastUpdated().format(DATE_FORMATTER) : 
                METADATA_UNKNOWN
            );
        }
        response.setCacheEnabled(true);
        
        return response;
    }
    
    /**
     * Initialize skill maps for a test type
     * @param testMap The map to initialize with skills
     */
    private void initializeSkillMaps(Map<String, Map<String, Object>> testMap) {
        testMap.put(SKILL_LISTENING, new HashMap<>());
        testMap.put(SKILL_READING, new HashMap<>());
        testMap.put(SKILL_WRITING, new HashMap<>());
        testMap.put(SKILL_SPEAKING, new HashMap<>());
    }
    
    /**
     * Process and add scores to the appropriate skill map
     * @param testMap The test map to add to
     * @param skill The language skill (listening, reading, etc.)
     * @param score The score or score range
     * @param clbLevel The CLB level
     */
    private void processScores(Map<String, Map<String, Object>> testMap, String skill, String score, Integer clbLevel) {
        if (score == null || score.isEmpty() || NOT_APPLICABLE.equalsIgnoreCase(score)) {
            return; // Skip empty or N/A values
        }
        
        Map<String, Object> skillMap = testMap.get(skill);
        if (score.contains(RANGE_SEPARATOR)) {
            // Handle ranges like "10-20"
            skillMap.put(score, clbLevel);
        } else {
            // Handle individual scores
            skillMap.put(score, clbLevel);
        }
    }
    
    /**
     * Convert a test score to CLB level
     * @param testType The language test type (CELPIP, IELTS, etc.)
     * @param skill The language skill (listening, reading, etc.)
     * @param score The score value
     * @return The CLB level or null if not found
     */
    public Integer convertToCLB(String testType, String skill, String score) {
        if (testType == null || skill == null || score == null) {
            return null;
        }
        
        logger.info("Converting score: testType={}, skill={}, score={}", testType, skill, score);
        
        CLBConversionResponse tables = getConversionTables();
        Map<String, Map<String, Object>> testMap = getTestMap(tables, testType.toUpperCase());
        if (testMap == null) {
            logger.warn("No test map found for test type: {}", testType);
            return null;
        }
        
        Map<String, Object> skillMap = testMap.get(skill.toLowerCase());
        if (skillMap == null) {
            logger.warn("No skill map found for skill: {}", skill);
            return null;
        }
        
        // First, try exact match
        if (skillMap.containsKey(score)) {
            logger.info("Found exact match for score: {}", score);
            return (Integer) skillMap.get(score);
        }
        
        // If the score itself contains a range (e.g., "217-248"), we need to match it against ranges in the map
        if (score.contains(RANGE_SEPARATOR)) {
            // The incoming score is already a range like "217-248", try direct lookup from the map
            logger.info("Input score is a range: {}", score);
            for (Map.Entry<String, Object> entry : skillMap.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(score)) {
                    logger.info("Found range match for score: {}", score);
                    return (Integer) entry.getValue();
                }
            }
            
            // If no exact match for the range, try to see if the ranges overlap
            try {
                String[] inputRange = score.split(RANGE_SEPARATOR);
                double inputMin = Double.parseDouble(inputRange[0]);
                double inputMax = Double.parseDouble(inputRange[1]);
                
                for (Map.Entry<String, Object> entry : skillMap.entrySet()) {
                    String key = entry.getKey();
                    if (key.contains(RANGE_SEPARATOR)) {
                        String[] mapRange = key.split(RANGE_SEPARATOR);
                        double mapMin = Double.parseDouble(mapRange[0]);
                        double mapMax = Double.parseDouble(mapRange[1]);
                        
                        // Check if ranges overlap significantly
                        if ((inputMin >= mapMin && inputMin <= mapMax) ||
                            (inputMax >= mapMin && inputMax <= mapMax) ||
                            (mapMin >= inputMin && mapMin <= inputMax)) {
                            logger.info("Found overlapping range match: {} overlaps with {}", score, key);
                            return (Integer) entry.getValue();
                        }
                    }
                }
            } catch (NumberFormatException e) {
                logger.warn(LOG_PARSING_ERROR, score);
            }
        } else {
            // Try to parse as number and match against ranges
            try {
                double numScore = Double.parseDouble(score);
                logger.info("Trying numeric match for score: {}", numScore);
                
                for (Map.Entry<String, Object> entry : skillMap.entrySet()) {
                    String key = entry.getKey();
                    if (key.contains(RANGE_SEPARATOR)) {
                        String[] range = key.split(RANGE_SEPARATOR);
                        double min = Double.parseDouble(range[0]);
                        double max = Double.parseDouble(range[1]);
                        if (numScore >= min && numScore <= max) {
                            logger.info("Found range match: {} is within {}-{}", numScore, min, max);
                            return (Integer) entry.getValue();
                        }
                    } else {
                        // Also check single values
                        try {
                            double mapValue = Double.parseDouble(key);
                            if (Math.abs(numScore - mapValue) < 0.001) { // Use epsilon for floating point comparison
                                logger.info("Found approximate match: {} ≈ {}", numScore, mapValue);
                                return (Integer) entry.getValue();
                            }
                        } catch (NumberFormatException ignored) {
                            // Not a number, ignore
                        }
                    }
                }
            } catch (NumberFormatException e) {
                logger.warn(LOG_PARSING_ERROR, score);
            }
        }
        
        logger.warn("No match found for score: {}", score);
        return null;
    }
    
    /**
     * Get the appropriate test map based on test type
     * @param tables The conversion tables
     * @param testType The test type
     * @return The corresponding test map
     */
    private Map<String, Map<String, Object>> getTestMap(CLBConversionResponse tables, String testType) {
        return switch (testType) {
            case TEST_CELPIP -> tables.getCelpip();
            case TEST_IELTS -> tables.getIelts();
            case TEST_PTE -> tables.getPte();
            case TEST_TEF -> tables.getTef();
            case TEST_TCF -> tables.getTcf();
            default -> null;
        };
    }
    
    /**
     * Get all possible score options for each test type and skill
     * @return Map of test types to skills to score values
     */
    @Cacheable("scoringOptions")
    public Map<String, Map<String, List<String>>> getScoreOptions() {
        List<CLBTestConverter> conversions = getAllConversions();
        Map<String, Map<String, List<String>>> result = new HashMap<>();
        
        // Initialize maps for test types
        Map<String, List<String>> celpipOptions = new HashMap<>();
        Map<String, List<String>> ieltsOptions = new HashMap<>();
        Map<String, List<String>> pteOptions = new HashMap<>();
        Map<String, List<String>> tefOptions = new HashMap<>();
        Map<String, List<String>> tcfOptions = new HashMap<>();
        
        // Initialize skill lists for each test type
        initializeSkillLists(celpipOptions);
        initializeSkillLists(ieltsOptions);
        initializeSkillLists(pteOptions);
        initializeSkillLists(tefOptions);
        initializeSkillLists(tcfOptions);
        
        // Process all conversions to collect unique score values
        for (CLBTestConverter conversion : conversions) {
            // Process CELPIP scores
            processScoreOption(celpipOptions, SKILL_LISTENING, conversion.getCelpipListening());
            processScoreOption(celpipOptions, SKILL_READING, conversion.getCelpipReading());
            processScoreOption(celpipOptions, SKILL_WRITING, conversion.getCelpipWriting());
            processScoreOption(celpipOptions, SKILL_SPEAKING, conversion.getCelpipSpeaking());
            
            // Process IELTS scores
            processScoreOption(ieltsOptions, SKILL_LISTENING, conversion.getIeltsListening());
            processScoreOption(ieltsOptions, SKILL_READING, conversion.getIeltsReading());
            processScoreOption(ieltsOptions, SKILL_WRITING, conversion.getIeltsWriting());
            processScoreOption(ieltsOptions, SKILL_SPEAKING, conversion.getIeltsSpeaking());
            
            // Process PTE scores
            processScoreOption(pteOptions, SKILL_LISTENING, conversion.getPteListening());
            processScoreOption(pteOptions, SKILL_READING, conversion.getPteReading());
            processScoreOption(pteOptions, SKILL_WRITING, conversion.getPteWriting());
            processScoreOption(pteOptions, SKILL_SPEAKING, conversion.getPteSpeaking());
            
            // Process TEF scores
            processScoreOption(tefOptions, SKILL_LISTENING, conversion.getTefListening());
            processScoreOption(tefOptions, SKILL_READING, conversion.getTefReading());
            processScoreOption(tefOptions, SKILL_WRITING, conversion.getTefWriting());
            processScoreOption(tefOptions, SKILL_SPEAKING, conversion.getTefSpeaking());
            
            // Process TCF scores
            processScoreOption(tcfOptions, SKILL_LISTENING, conversion.getTcfListening());
            processScoreOption(tcfOptions, SKILL_READING, conversion.getTcfReading());
            processScoreOption(tcfOptions, SKILL_WRITING, conversion.getTcfWriting());
            processScoreOption(tcfOptions, SKILL_SPEAKING, conversion.getTcfSpeaking());
        }
        
        // Sort all option lists to ensure consistent order
        sortAllOptions(celpipOptions);
        sortAllOptions(ieltsOptions);
        sortAllOptions(pteOptions);
        sortAllOptions(tefOptions);
        sortAllOptions(tcfOptions);
        
        // Set result maps
        result.put(TEST_CELPIP, celpipOptions);
        result.put(TEST_IELTS, ieltsOptions);
        result.put(TEST_PTE, pteOptions);
        result.put(TEST_TEF, tefOptions);
        result.put(TEST_TCF, tcfOptions);
        
        return result;
    }
    
    /**
     * Initialize skill lists for options
     * @param skillMap The map to initialize
     */
    private void initializeSkillLists(Map<String, List<String>> skillMap) {
        skillMap.put(SKILL_LISTENING, new ArrayList<>());
        skillMap.put(SKILL_READING, new ArrayList<>());
        skillMap.put(SKILL_WRITING, new ArrayList<>());
        skillMap.put(SKILL_SPEAKING, new ArrayList<>());
    }
    
    /**
     * Process a score option and add to the list if valid and not already present
     * @param optionsMap The options map to add to
     * @param skill The language skill
     * @param score The score value
     */
    private void processScoreOption(Map<String, List<String>> optionsMap, String skill, String score) {
        if (score == null || score.isEmpty() || NOT_APPLICABLE.equalsIgnoreCase(score)) {
            return; // Skip empty or N/A values
        }
        
        List<String> options = optionsMap.get(skill);
        if (!options.contains(score)) {
            options.add(score);
        }
    }
    
    /**
     * Sort all option lists for consistent ordering
     * @param optionsMap The map containing lists to sort
     */
    private void sortAllOptions(Map<String, List<String>> optionsMap) {
        for (List<String> options : optionsMap.values()) {
            // Sort numerically if possible, otherwise lexicographically
            options.sort((a, b) -> {
                try {
                    // For single numeric values
                    double aVal = Double.parseDouble(a);
                    double bVal = Double.parseDouble(b);
                    return Double.compare(aVal, bVal);
                } catch (NumberFormatException e) {
                    // For ranges, use string comparison as fallback
                    if (a.contains(RANGE_SEPARATOR) && b.contains(RANGE_SEPARATOR)) {
                        try {
                            // Compare by lower range bound
                            double aLower = Double.parseDouble(a.split(RANGE_SEPARATOR)[0]);
                            double bLower = Double.parseDouble(b.split(RANGE_SEPARATOR)[0]);
                            return Double.compare(aLower, bLower);
                        } catch (NumberFormatException ex) {
                            // Fall back to string comparison
                            return a.compareTo(b);
                        }
                    } else {
                        return a.compareTo(b);
                    }
                }
            });
        }
    }
} 