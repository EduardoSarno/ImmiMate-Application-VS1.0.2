package co.immimate.auth.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

/**
 * Filter to implement rate limiting on authentication endpoints
 * to prevent brute force attacks.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);
    
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    // Rate limiting configuration
    private static final int CAPACITY = 5; // 5 requests initially available
    private static final int REFILL_TOKENS = 5; // 5 tokens refilled
    private static final int REFILL_MINUTES = 1; // tokens refilled every 1 minute
    
    // Protected endpoints patterns
    private static final String LOGIN_ENDPOINT = "/api/auth/login";
    private static final String REGISTER_ENDPOINT = "/api/auth/register";
    private static final String VERIFY_ENDPOINT = "/api/auth/verify";
    
    // Test environment control
    private static final String TEST_RATE_LIMIT_HEADER = "X-Test-Rate-Limit";

    @Autowired
    private Environment environment;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        // Special handling for test environment
        if (isTestEnvironment()) {
            // Check for special test header to explicitly enable rate limiting
            String testRateLimitHeader = request.getHeader(TEST_RATE_LIMIT_HEADER);
            boolean enforceRateLimitForTest = "true".equals(testRateLimitHeader);
            
            if (!enforceRateLimitForTest) {
                // By default, skip rate limiting in test environment unless header is present
                filterChain.doFilter(request, response);
                return;
            }
            // If header is present with "true", continue with rate limiting even in test environment
            logger.debug("Test environment with rate limiting explicitly enabled");
        }
        
        // Skip rate limiting for non-authentication endpoints
        String requestURI = request.getRequestURI();
        if (!isAuthenticationEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Get client IP address
        String clientIP = getClientIP(request);
        
        // Get or create bucket for this IP
        Bucket bucket = buckets.computeIfAbsent(clientIP, this::createNewBucket);
        
        // Try to consume a token
        if (bucket.tryConsume(1)) {
            // Request allowed, proceed
            logger.debug("Rate limit allowed for IP: {}", clientIP);
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            logger.warn("Rate limit exceeded for IP: {}", clientIP);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\",\"status\":429}");
        }
    }
    
    /**
     * Check if we're running in a test environment
     */
    private boolean isTestEnvironment() {
        if (environment != null) {
            for (String profile : environment.getActiveProfiles()) {
                if ("test".equals(profile)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Create a new token bucket with specified capacity and refill rate
     */
    private Bucket createNewBucket(String clientIP) {
        logger.debug("Creating rate limit bucket for IP: {}", clientIP);
        return Bucket.builder()
                .addLimit(Bandwidth.classic(CAPACITY, Refill.intervally(REFILL_TOKENS, Duration.ofMinutes(REFILL_MINUTES))))
                .build();
    }
    
    /**
     * Check if the current request URI is for an authentication endpoint that should be rate limited
     */
    private boolean isAuthenticationEndpoint(String uri) {
        return uri.equals(LOGIN_ENDPOINT) || 
               uri.equals(REGISTER_ENDPOINT) || 
               uri.equals(VERIFY_ENDPOINT);
    }
    
    /**
     * Get client IP address, taking into account possible proxy headers
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs (client, proxy1, proxy2, ...), get the first one
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
} 