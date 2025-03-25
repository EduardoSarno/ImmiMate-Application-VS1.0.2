package co.immimate.api;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller providing health check endpoints.
 * This is used both for application monitoring and for testing purposes,
 * such as security header tests.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    /**
     * Simple health check endpoint.
     * Returns 200 OK with basic application status information.
     * Used by the security headers tests.
     * 
     * @return Response containing basic health status
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", "UP");
        healthStatus.put("timestamp", System.currentTimeMillis());
        healthStatus.put("environment", System.getProperty("spring.profiles.active", "default"));
        
        logger.debug("Health check endpoint called");
        
        return ResponseEntity.ok(healthStatus);
    }
} 