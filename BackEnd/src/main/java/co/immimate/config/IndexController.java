package co.immimate.config;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling static content and root path
 * This is also used to set a CSRF token cookie for the application
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:3000"}, allowedHeaders = "*", allowCredentials = "true")
public class IndexController {
    
    // Response constants
    private static final String STATUS_KEY = "status";
    private static final String MESSAGE_KEY = "message";
    private static final String STATUS_OK = "OK";
    private static final String API_RUNNING_MESSAGE = "ImmiMate API is running";
    
    // CSRF response constants
    private static final String CSRF_KEY = "csrf";
    private static final String TOKEN_KEY = "token";
    private static final String HEADER_NAME_KEY = "headerName";
    private static final String PARAMETER_NAME_KEY = "parameterName";
    private static final String CSRF_TOKEN_MESSAGE = "A CSRF token has been set in the cookies";

    @Autowired
    private CsrfTokenRepository csrfTokenRepository;

    /**
     * Root endpoint to serve as a health check and to set a CSRF token
     * 
     * @return Simple response with status message
     */
    @GetMapping("/")
    public ResponseEntity<?> index(HttpServletRequest request, HttpServletResponse response) {
        // Generate and save CSRF token
        CsrfToken csrfToken = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(csrfToken, request, response);
        
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put(STATUS_KEY, STATUS_OK);
        responseBody.put(MESSAGE_KEY, API_RUNNING_MESSAGE);
        
        return ResponseEntity.ok(responseBody);
    }
    
    /**
     * Endpoint to provide CSRF token for testing
     * Spring Security will automatically add the CSRF token cookie
     * 
     * @return Simple response with CSRF info
     */
    @GetMapping("/csrf-info")
    public ResponseEntity<?> csrfToken(HttpServletRequest request, HttpServletResponse response) {
        // Generate and save CSRF token
        CsrfToken csrfToken = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(csrfToken, request, response);
        
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put(STATUS_KEY, STATUS_OK);
        responseBody.put(CSRF_KEY, CSRF_TOKEN_MESSAGE);
        responseBody.put(TOKEN_KEY, csrfToken.getToken());
        responseBody.put(HEADER_NAME_KEY, csrfToken.getHeaderName());
        responseBody.put(PARAMETER_NAME_KEY, csrfToken.getParameterName());
        
        return ResponseEntity.ok(responseBody);
    }
} 