package co.immimate.auth.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import co.immimate.user.service.UserDetailsServiceImpl;

/**
 * Configuration class for Spring Security
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    // CSRF configuration constants
    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    
    // Endpoint paths constants
    private static final String AUTH_LOGIN_PATH = "/api/auth/login";
    private static final String AUTH_REGISTER_PATH = "/api/auth/register";
    private static final String AUTH_LOGOUT_PATH = "/api/auth/logout";
    private static final String AUTH_PATH_PATTERN = "/api/auth/**";
    private static final String OAUTH2_PATH_PATTERN = "/api/oauth2/**";
    private static final String HEALTH_ENDPOINT = "/api/health";
    private static final String CSRF_ENDPOINT = "/api/csrf";
    private static final String CSRF_INFO_ENDPOINT = "/api/csrf-info";
    private static final String OAUTH2_SUCCESS_PATH = "/api/oauth2/login/success";
    private static final String LANGUAGE_TESTS_PATH_PATTERN = "/api/language-tests/**";
    
    // CORS configuration constants
    private static final String LOCALHOST_3000 = "http://localhost:3000";
    private static final String LOCALHOST_WILDCARD = "http://localhost:*";
    private static final String LOCALHOST_IP_WILDCARD = "http://127.0.0.1:*";
    private static final String CORS_ALL_PATHS = "/**";
    private static final long CORS_MAX_AGE_SECONDS = 3600L;
    
    // CORS allowed methods
    private static final String HTTP_GET = "GET";
    private static final String HTTP_POST = "POST";
    private static final String HTTP_PUT = "PUT";
    private static final String HTTP_PATCH = "PATCH";
    private static final String HTTP_DELETE = "DELETE";
    private static final String HTTP_OPTIONS = "OPTIONS";
    
    // CORS allowed headers
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_XSRF_TOKEN = "X-XSRF-TOKEN";
    private static final String HEADER_ACCEPT = "Accept";
    private static final String HEADER_ORIGIN = "Origin";
    private static final String HEADER_X_REQUESTED_WITH = "X-Requested-With";
    private static final String HEADER_X_DEBUG_INFO = "x-debug-info";
    
    // OAuth2 configuration
    private static final String OAUTH2_LOGIN_PAGE = "http://localhost:3000/login";

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtAuthFilter jwtAuthFilter;
    
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Configure user details service and password encoder
     */
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService)
            .passwordEncoder(passwordEncoder);
    }

    /**
     * Bean for authentication manager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    /**
     * Bean for CSRF token repository
     */
    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        tokenRepository.setCookieName(CSRF_COOKIE_NAME);
        tokenRepository.setHeaderName(CSRF_HEADER_NAME);
        return tokenRepository;
    }
    
    /**
     * Bean for rate limiting filter
     */
    @Bean
    public RateLimitFilter rateLimitFilter() {
        return new RateLimitFilter();
    }

    /**
     * Configure HTTP security
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors().configurationSource(corsConfigurationSource()).and()
            // Re-enable CSRF protection with exceptions for auth endpoints
            .csrf()
                .csrfTokenRepository(csrfTokenRepository())
                // Specifically exempt authentication endpoints from CSRF protection
                .ignoringAntMatchers(AUTH_LOGIN_PATH, AUTH_REGISTER_PATH, AUTH_LOGOUT_PATH, 
                                     OAUTH2_PATH_PATTERN, CSRF_ENDPOINT, HEALTH_ENDPOINT, CSRF_INFO_ENDPOINT)
                .and()
            // Add security headers including CSP
            .headers()
                .contentSecurityPolicy("default-src 'self'; script-src 'self' 'unsafe-inline' http://localhost:3000 http://localhost:8080 https://localhost:3000 https://localhost:8080; style-src 'self' 'unsafe-inline' http://localhost:3000 http://localhost:8080 https://localhost:3000 https://localhost:8080; img-src 'self' data:; font-src 'self'; connect-src 'self' http://localhost:* http://127.0.0.1:* https://localhost:* https://127.0.0.1:*; frame-ancestors 'none'; form-action 'self'; base-uri 'self'; object-src 'none'; upgrade-insecure-requests")
                .and()
                .frameOptions().deny()
                .xssProtection().block(true)
                .and()
                .contentTypeOptions()
                .and()
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                .and()
                .permissionsPolicy(permissions -> permissions
                    .policy("camera=(), microphone=(), geolocation=(), interest-cohort=()")
                )
                .and()
            .and()
            .exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint).and()
            // Use STATEFUL session management for OAuth2
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED).and()
            .authorizeRequests()
                .antMatchers(AUTH_PATH_PATTERN).permitAll()
                .antMatchers(OAUTH2_PATH_PATTERN).permitAll()
                .antMatchers(HEALTH_ENDPOINT).permitAll()
                .antMatchers(CSRF_ENDPOINT).permitAll()
                .antMatchers(CSRF_INFO_ENDPOINT).permitAll()
                .antMatchers(LANGUAGE_TESTS_PATH_PATTERN).permitAll()
                .anyRequest().authenticated()
                .and()
            .oauth2Login()
                .loginPage(OAUTH2_LOGIN_PAGE)
                .authorizationEndpoint()
                    .baseUri("/api/oauth2/authorization")
                    .and()
                .redirectionEndpoint()
                    .baseUri("/api/login/oauth2/code/*")
                    .and()
                .successHandler(oauth2AuthenticationSuccessHandler())
                .permitAll();

        // Add JWT filter before UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        // Add rate limiting filter
        http.addFilterBefore(rateLimitFilter(), JwtAuthFilter.class);
        
        return http.build();
    }

    /**
     * Bean for CORS configuration
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(LOCALHOST_3000, LOCALHOST_WILDCARD, LOCALHOST_IP_WILDCARD));
        configuration.setAllowedMethods(Arrays.asList(HTTP_GET, HTTP_POST, HTTP_PUT, HTTP_PATCH, HTTP_DELETE, HTTP_OPTIONS));
        configuration.setAllowedHeaders(Arrays.asList(HEADER_AUTHORIZATION, HEADER_CONTENT_TYPE, HEADER_XSRF_TOKEN, 
                                                      HEADER_ACCEPT, HEADER_ORIGIN, HEADER_X_REQUESTED_WITH, HEADER_X_DEBUG_INFO));
        configuration.setExposedHeaders(Arrays.asList(HEADER_XSRF_TOKEN));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(CORS_MAX_AGE_SECONDS);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(CORS_ALL_PATHS, configuration);
        return source;
    }

    /**
     * Custom success handler for OAuth2 login
     */
    @Bean
    public AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler() {
        // Fix the path to include the API context path
        return new SimpleUrlAuthenticationSuccessHandler(OAUTH2_SUCCESS_PATH);
    }
} 