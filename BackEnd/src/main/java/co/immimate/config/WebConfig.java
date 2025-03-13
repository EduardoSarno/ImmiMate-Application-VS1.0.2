package co.immimate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for web-related settings including CORS and serving static resources
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configure CORS mapping for the application
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*") // For development, restrict in production
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .maxAge(3600); // 1 hour
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Handle the root path and index.html
        registry.addResourceHandler("/", "index.html")
                .addResourceLocations("classpath:/static/", "file:../Front-End/Html/");
        
        // Handle requests for the HTML files
        registry.addResourceHandler("/Html/**", "Html/**")
                .addResourceLocations("classpath:/static/Html/", "file:../Front-End/Html/")
                .setCachePeriod(3600);
                
        // Handle CSS files
        registry.addResourceHandler("/css/**", "css/**")
                .addResourceLocations("classpath:/static/css/", "file:../Front-End/css/")
                .setCachePeriod(3600);
                
        // Handle JavaScript files
        registry.addResourceHandler("/JavaScript/**", "JavaScript/**", "/js/**")
                .addResourceLocations("classpath:/static/JavaScript/", "file:../Front-End/JavaScript/")
                .setCachePeriod(3600);
                
        // Handle Data files - this was causing the conflict
        registry.addResourceHandler("/Data/**", "Data/**")
                .addResourceLocations("classpath:/static/Data/", "file:../Front-End/Data/")
                .setCachePeriod(3600);
                
        // Handle image files
        registry.addResourceHandler("/images/**", "images/**")
                .addResourceLocations("classpath:/static/images/", "file:../Front-End/images/", "file:../Front-End/Data/Images/")
                .setCachePeriod(3600);
                
        // Add handler for Front-End/** paths
        registry.addResourceHandler("/Front-End/**", "Front-End/**")
                .addResourceLocations("file:../Front-End/")
                .setCachePeriod(3600);
    }
} 