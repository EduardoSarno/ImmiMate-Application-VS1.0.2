package co.immimate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration for PasswordEncoder bean to avoid circular dependencies
 */
@Configuration
public class PasswordEncoderConfig {
    
    // BCrypt strength factor (default is 10, higher is stronger but slower)
    private static final int BCRYPT_STRENGTH = 12;

    /**
     * Bean for password encoder using BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }
} 