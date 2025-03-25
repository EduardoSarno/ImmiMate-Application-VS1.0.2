package co.immimate.test;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

/**
 * Test-specific security configuration that:
 * 1. Generates CSRF tokens but doesn't enforce checking
 * 2. Makes endpoints accessible for testing
 * 3. Preserves cookie handling for validation
 *
 * This configuration is activated only with the "test" profile
 * and takes precedence over the main security configuration.
 */
@Configuration
@Profile("test")
public class SecurityTestConfig {

    /**
     * Custom CSRF token repository that will generate tokens
     * but won't enforce CSRF protection for tests
     */
    @Bean
    @Primary
    public CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        tokenRepository.setCookieName("XSRF-TOKEN");
        tokenRepository.setHeaderName("X-XSRF-TOKEN");
        return tokenRepository;
    }
    
    /**
     * Filter to ensure CSRF token is included in response
     */
    private Filter csrfHeaderFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, 
                                           @NonNull FilterChain filterChain) throws ServletException, IOException {
                CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
                if (csrf != null) {
                    response.setHeader("X-CSRF-HEADER", csrf.getHeaderName());
                    response.setHeader("X-CSRF-PARAM", csrf.getParameterName());
                    response.setHeader("X-CSRF-TOKEN", csrf.getToken());
                }
                filterChain.doFilter(request, response);
            }
        };
    }

    /**
     * Test security configuration for integration tests
     * 
     * This generates CSRF tokens but does not enforce checking them
     * while still allowing the security filter chain to process authentication
     */
    @Bean
    @Primary
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Configure security for test environment
        http
            // Enable CSRF for token generation but make it non-enforcing for tests
            .csrf()
                .csrfTokenRepository(csrfTokenRepository())
                .ignoringAntMatchers("/**") // Ignore CSRF checking for all endpoints in tests
            .and()
            .addFilterAfter(csrfHeaderFilter(), CsrfFilter.class)
            // Use stateless session management
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            // Allow all endpoints in test mode for easier testing
            .authorizeRequests()
                .antMatchers("/**").permitAll();
            
        return http.build();
    }
} 