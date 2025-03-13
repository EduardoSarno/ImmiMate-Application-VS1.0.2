package co.immimate.test;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import co.immimate.user.model.User;
import co.immimate.user.repository.UserRepository;

/**
 * Provides test data initialization beans for integration tests.
 * This ensures a consistent test database state.
 */
@TestConfiguration
@Profile("test")
public class TestDataInitializer {

    @Bean
    public TestDataService testDataService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        return new TestDataService(userRepository, passwordEncoder);
    }
    
    /**
     * Service class to initialize and manage test data
     */
    public static class TestDataService {
        
        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        
        public TestDataService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
            this.userRepository = userRepository;
            this.passwordEncoder = passwordEncoder;
        }
        
        /**
         * Initializes the test database with required test data
         */
        public void initializeTestData() {
            cleanTestData();
            createTestUsers();
        }
        
        /**
         * Cleans only the specific test data created by tests
         * This preserves all other data and table structures
         */
        private void cleanTestData() {
            // Only delete specific test users by email
            userRepository.findByEmail(TestConstants.TEST_USER_EMAIL)
                .ifPresent(user -> userRepository.delete(user));
            userRepository.findByEmail(TestConstants.NEW_USER_EMAIL)
                .ifPresent(user -> userRepository.delete(user));
                
            // Log that we're only cleaning specific test data
            System.out.println("✅ Cleaned specific test data without dropping tables");
        }
        
        private void createTestUsers() {
            // Create main test user
            User testUser = new User();
            testUser.setEmail(TestConstants.TEST_USER_EMAIL);
            testUser.setPassword(passwordEncoder.encode(TestConstants.TEST_USER_PASSWORD));
            testUser.setPhoneNumber(TestConstants.TEST_USER_PHONE);
            testUser.setFirstName(TestConstants.TEST_USER_FIRST_NAME);
            testUser.setLastName(TestConstants.TEST_USER_LAST_NAME);
            testUser.setRole("USER");
            
            userRepository.save(testUser);
        }
    }
} 