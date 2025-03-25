package co.immimate.auth.security;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Filter to add security-related HTTP headers to all responses.
 * This implements content security policy (CSP) and other security headers
 * to enhance the application's security posture.
 */
@Component
@Order(1) // High priority to ensure headers are set before any response is sent
public class SecurityHeadersFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityHeadersFilter.class);
    
    // Security header names
    private static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    private static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    private static final String X_FRAME_OPTIONS = "X-Frame-Options";
    private static final String X_XSS_PROTECTION = "X-XSS-Protection";
    private static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
    private static final String REFERRER_POLICY = "Referrer-Policy";
    private static final String PERMISSIONS_POLICY = "Permissions-Policy";
    private static final String X_POWERED_BY = "X-Powered-By";
    
    // Security header values
    private static final String NO_SNIFF = "nosniff";
    private static final String DENY = "DENY";
    private static final String XSS_MODE = "1; mode=block";
    private static final String HSTS_VALUE = "max-age=31536000; includeSubDomains";
    private static final String REFERRER_VALUE = "strict-origin-when-cross-origin";
    private static final String PERMISSIONS_VALUE = "camera=(), microphone=(), geolocation=(), interest-cohort=()";
    
    // CSP directives
    private static final String DEFAULT_SRC = "default-src 'self'";
    private static final String SCRIPT_SRC = "script-src 'self' 'unsafe-inline' http://localhost:3000 http://localhost:8080 https://localhost:3000 https://localhost:8080";
    private static final String STYLE_SRC = "style-src 'self' 'unsafe-inline' http://localhost:3000 http://localhost:8080 https://localhost:3000 https://localhost:8080";
    private static final String IMG_SRC = "img-src 'self' data:";
    private static final String FONT_SRC = "font-src 'self'";
    private static final String CONNECT_SRC = "connect-src 'self' http://localhost:* http://127.0.0.1:* https://localhost:* https://127.0.0.1:*";
    private static final String FRAME_ANCESTORS = "frame-ancestors 'none'";
    private static final String FORM_ACTION = "form-action 'self'";
    private static final String BASE_URI = "base-uri 'self'";
    private static final String OBJECT_SRC = "object-src 'none'";
    private static final String UPGRADE_INSECURE_REQUESTS = "upgrade-insecure-requests";
    
    // CSP delimiter
    private static final String CSP_DELIMITER = "; ";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Add Content-Security-Policy header
        httpResponse.setHeader(CONTENT_SECURITY_POLICY, buildCspHeader());
        
        // Prevent MIME type sniffing
        httpResponse.setHeader(X_CONTENT_TYPE_OPTIONS, NO_SNIFF);
        
        // Prevent clickjacking
        httpResponse.setHeader(X_FRAME_OPTIONS, DENY);
        
        // Enable XSS protection
        httpResponse.setHeader(X_XSS_PROTECTION, XSS_MODE);
        
        // HTTP Strict Transport Security (when deployed with HTTPS)
        httpResponse.setHeader(STRICT_TRANSPORT_SECURITY, HSTS_VALUE);
        
        // Set referrer policy to reduce information leakage
        httpResponse.setHeader(REFERRER_POLICY, REFERRER_VALUE);
        
        // Limit browser features
        httpResponse.setHeader(PERMISSIONS_POLICY, PERMISSIONS_VALUE);
        
        // Remove information disclosure headers
        // Note: We need to hide this header, not just set an empty value
        // Tomcat/Spring may add X-Powered-By by default
        httpResponse.setHeader(X_POWERED_BY, null);
        
        // Log at debug level only to avoid cluttering logs
        logger.debug("Security headers added to response");
        
        // Continue the filter chain
        chain.doFilter(request, response);
    }
    
    /**
     * Builds the Content Security Policy header value.
     * This defines restrictions on external resources that can be loaded.
     * 
     * @return The constructed CSP header value
     */
    private String buildCspHeader() {
        return String.join(CSP_DELIMITER, 
                DEFAULT_SRC,
                SCRIPT_SRC,
                STYLE_SRC,
                IMG_SRC,
                FONT_SRC,
                CONNECT_SRC,
                FRAME_ANCESTORS,
                FORM_ACTION,
                BASE_URI,
                OBJECT_SRC,
                UPGRADE_INSECURE_REQUESTS
        );
    }
} 