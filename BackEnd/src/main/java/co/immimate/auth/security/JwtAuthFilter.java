package co.immimate.auth.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import co.immimate.user.service.UserDetailsServiceImpl;

/**
 * Filter for intercepting requests and validating JWT tokens
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    /**
     * Intercepts incoming requests and validates JWT tokens
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        final String authorizationHeader = request.getHeader("Authorization");
        
        // Enhanced logging for request details
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        log.info("Processing request: {} {} from IP: {}", method, requestURI, request.getRemoteAddr());
        
        if (authorizationHeader != null) {
            String headerStart = authorizationHeader.length() > 15 ? 
                    authorizationHeader.substring(0, 15) + "..." : authorizationHeader;
            log.info("Authorization header: Present (starts with: {})", headerStart);
        } else {
            log.info("Authorization header: Not present");
        }

        // Log all headers for debugging
        log.debug("Request headers:");
        request.getHeaderNames().asIterator().forEachRemaining(headerName -> 
            log.debug("  {}: {}", headerName, request.getHeader(headerName)));

        String email = null;
        String jwt = null;

        // Extract the token from the Authorization header
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            log.info("JWT token extracted, length: {}", jwt.length());
            try {
                email = jwtUtil.extractEmail(jwt);
                log.info("Email extracted from token: {}", email);
            } catch (Exception e) {
                log.error("Error extracting email from token: {}", e.getMessage());
                // Don't send unauthorized response here, let the AuthenticationEntryPoint handle it
            }
        } else if (authorizationHeader != null) {
            log.warn("Authorization header present but doesn't start with 'Bearer '");
        }

        // Validate the token and set up security context
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            log.info("Loading UserDetails for email: {}", email);
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(email);
                log.info("UserDetails loaded successfully: {}", userDetails.getUsername());

                if (jwtUtil.validateToken(jwt, userDetails)) {
                    log.info("JWT token validated successfully for user: {}", email);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("Authentication set in SecurityContext for user: {}", email);
                } else {
                    log.warn("JWT token validation failed for email: {}", email);
                }
            } catch (Exception e) {
                log.error("Error processing authentication: {}", e.getMessage(), e);
            }
        } else if (email == null && jwt != null) {
            log.warn("Could not extract email from token");
        }
        
        filterChain.doFilter(request, response);
    }
} 