package co.immimate.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import co.immimate.auth.security.SecurityHeadersFilter;

/**
 * Configuration class for registering security headers filter.
 * This ensures our security headers are applied to all incoming requests.
 * Part of the phase 3 security enhancements (Content Security Policy and Security Headers).
 */
@Configuration
public class SecurityHeadersConfig {
    
    // Filter registration constants
    private static final String ALL_URL_PATTERNS = "/*";
    private static final String SECURITY_HEADERS_FILTER_NAME = "securityHeadersFilter";

    @Autowired
    private SecurityHeadersFilter securityHeadersFilter;

    /**
     * Register the SecurityHeadersFilter as a servlet filter with high priority.
     * This ensures it's applied to all requests, integrated with the existing security config.
     * 
     * @return The filter registration bean
     */
    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilterRegistration() {
        FilterRegistrationBean<SecurityHeadersFilter> registrationBean = new FilterRegistrationBean<>();
        
        // Use the autowired filter instance to ensure Spring context manages it properly
        registrationBean.setFilter(securityHeadersFilter);
        
        // Apply to all URL patterns
        registrationBean.addUrlPatterns(ALL_URL_PATTERNS);
        
        // Set it to run early in the filter chain
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        
        // Set a unique name for the filter
        registrationBean.setName(SECURITY_HEADERS_FILTER_NAME);
        
        return registrationBean;
    }
} 