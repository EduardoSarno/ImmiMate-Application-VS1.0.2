package co.immimate.profile.dto;

import java.util.Map;

import lombok.Data;

/**
 * DTO for returning language test conversion data to clients
 */
@Data
public class CLBConversionResponse {
    // Structure for each test type
    private Map<String, Map<String, Object>> celpip;
    private Map<String, Map<String, Object>> ielts;
    private Map<String, Map<String, Object>> pte;
    private Map<String, Map<String, Object>> tef;
    private Map<String, Map<String, Object>> tcf;
    
    // Metadata
    private String lastUpdated;
    private Boolean cacheEnabled;
} 