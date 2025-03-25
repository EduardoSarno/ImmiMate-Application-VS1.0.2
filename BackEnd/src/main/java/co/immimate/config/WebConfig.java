package co.immimate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for web-related settings including CORS and serving static resources
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    // CORS configuration constants
    private static final String[] ALLOWED_ORIGIN_PATTERNS = {"http://localhost:3000", "http://localhost:*", "http://127.0.0.1:*"};
    private static final String[] ALLOWED_METHODS = {"GET", "POST", "PUT", "DELETE", "OPTIONS"};
    private static final String[] ALLOWED_HEADERS = {"Authorization", "Content-Type", "X-XSRF-TOKEN"};
    private static final String[] EXPOSED_HEADERS = {"X-XSRF-TOKEN"};
    private static final int CORS_MAX_AGE_SECONDS = 3600; // 1 hour
    
    // Resource handler paths constants
    private static final String[] ROOT_PATTERNS = {"/", "index.html"};
    private static final String[] HTML_PATTERNS = {"/Html/**", "Html/**"};
    private static final String[] CSS_PATTERNS = {"/css/**", "css/**"};
    private static final String[] JS_PATTERNS = {"/JavaScript/**", "JavaScript/**", "/js/**"};
    private static final String[] DATA_PATTERNS = {"/Data/**", "Data/**"};
    private static final String[] IMAGE_PATTERNS = {"/images/**", "images/**"};
    private static final String[] FRONTEND_PATTERNS = {"/Front-End/**", "Front-End/**"};
    
    // Resource locations constants
    private static final String[] ROOT_LOCATIONS = {"classpath:/static/", "file:../Front-End/Html/"};
    private static final String[] HTML_LOCATIONS = {"classpath:/static/Html/", "file:../Front-End/Html/"};
    private static final String[] CSS_LOCATIONS = {"classpath:/static/css/", "file:../Front-End/css/"};
    private static final String[] JS_LOCATIONS = {"classpath:/static/JavaScript/", "file:../Front-End/JavaScript/"};
    private static final String[] DATA_LOCATIONS = {"classpath:/static/Data/", "file:../Front-End/Data/"};
    private static final String[] IMAGE_LOCATIONS = {"classpath:/static/images/", "file:../Front-End/images/", "file:../Front-End/Data/Images/"};
    private static final String[] FRONTEND_LOCATIONS = {"file:../Front-End/"};
    
    // Cache period constants
    private static final int CACHE_PERIOD_SECONDS = 3600; // 1 hour

    /**
     * Configure CORS mapping for the application
     */
    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS)
                .allowedMethods(ALLOWED_METHODS)
                .allowedHeaders(ALLOWED_HEADERS)
                .exposedHeaders(EXPOSED_HEADERS)
                .allowCredentials(true) // Enable credentials for cookie-based auth
                .maxAge(CORS_MAX_AGE_SECONDS);
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // Handle the root path and index.html
        registry.addResourceHandler(ROOT_PATTERNS)
                .addResourceLocations(ROOT_LOCATIONS);
        
        // Handle requests for the HTML files
        registry.addResourceHandler(HTML_PATTERNS)
                .addResourceLocations(HTML_LOCATIONS)
                .setCachePeriod(CACHE_PERIOD_SECONDS);
                
        // Handle CSS files
        registry.addResourceHandler(CSS_PATTERNS)
                .addResourceLocations(CSS_LOCATIONS)
                .setCachePeriod(CACHE_PERIOD_SECONDS);
                
        // Handle JavaScript files
        registry.addResourceHandler(JS_PATTERNS)
                .addResourceLocations(JS_LOCATIONS)
                .setCachePeriod(CACHE_PERIOD_SECONDS);
                
        // Handle Data files - this was causing the conflict
        registry.addResourceHandler(DATA_PATTERNS)
                .addResourceLocations(DATA_LOCATIONS)
                .setCachePeriod(CACHE_PERIOD_SECONDS);
                
        // Handle image files
        registry.addResourceHandler(IMAGE_PATTERNS)
                .addResourceLocations(IMAGE_LOCATIONS)
                .setCachePeriod(CACHE_PERIOD_SECONDS);
                
        // Add handler for Front-End/** paths
        registry.addResourceHandler(FRONTEND_PATTERNS)
                .addResourceLocations(FRONTEND_LOCATIONS)
                .setCachePeriod(CACHE_PERIOD_SECONDS);
    }
} 