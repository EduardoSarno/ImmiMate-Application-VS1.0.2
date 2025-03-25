package co.immimate.auth.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for CSRF token management
 */
@RestController
@RequestMapping("/api")
public class CsrfController {
    
    // API endpoints
    private static final String CSRF_ENDPOINT = "/csrf";
    
    // CSRF token cookie and response properties
    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_COOKIE_PATH = "/";
    private static final boolean CSRF_COOKIE_HTTP_ONLY = false;
    
    // Response map keys
    private static final String RESPONSE_TOKEN_KEY = "token";
    private static final String RESPONSE_HEADER_NAME_KEY = "headerName";

    /**
     * Endpoint to fetch a CSRF token.
     * This is useful for client-side applications that need to include CSRF tokens in requests.
     *
     * @param request The HTTP request
     * @param response The HTTP response
     * @return A response containing the CSRF token
     */
    @GetMapping(CSRF_ENDPOINT)
    public ResponseEntity<Map<String, String>> getCsrfToken(
            HttpServletRequest request, HttpServletResponse response) {
        
        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        
        if (csrf != null) {
            // Create a cookie for the CSRF token
            Cookie cookie = new Cookie(CSRF_COOKIE_NAME, csrf.getToken());
            cookie.setPath(CSRF_COOKIE_PATH);
            cookie.setHttpOnly(CSRF_COOKIE_HTTP_ONLY); // Allow JavaScript access to this cookie
            response.addCookie(cookie);
            
            // Also return the token in the response body
            Map<String, String> responseMap = new HashMap<>();
            responseMap.put(RESPONSE_TOKEN_KEY, csrf.getToken());
            responseMap.put(RESPONSE_HEADER_NAME_KEY, csrf.getHeaderName());
            
            return new ResponseEntity<>(responseMap, HttpStatus.OK);
        }
        
        // If no CSRF token is available, return an empty response
        return new ResponseEntity<>(new HashMap<>(), HttpStatus.OK);
    }
} 