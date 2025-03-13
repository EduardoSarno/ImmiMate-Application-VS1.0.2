package co.immimate.test;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

/**
 * Test configuration that imports all test-related configurations.
 */
@TestConfiguration
@Profile("test")
@Import(TestDataInitializer.class)
public class TestConfig {
    
    /**
     * Provides a RestTemplate bean for tests
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
} 