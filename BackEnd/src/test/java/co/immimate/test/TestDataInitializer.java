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
        private final String testUserEmail;
        private final String newUserEmail;
        
        public TestDataService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
            this.userRepository = userRepository;
            this.passwordEncoder = passwordEncoder;
            // Generate random emails for this test session
            this.testUserEmail = TestConstants.generateTestUserEmail();
            this.newUserEmail = TestConstants.generateNewUserEmail();
            System.out.println("⚡ Initialized TestDataService with primary email: " + testUserEmail);
        }
        
        /**
         * Returns the test user email for this test session
         */
        public String getTestUserEmail() {
            return testUserEmail;
        }
        
        /**
         * Returns the new user email for this test session
         */
        public String getNewUserEmail() {
            return newUserEmail;
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
            try {
                // We don't need to clean any specific users since we're using random emails
                // But we'll log it for clarity
                System.out.println("✅ Using random emails for test users, no cleanup needed");
            } catch (Exception e) {
                System.err.println("⚠️ Error with test data preparation: " + e.getMessage());
            }
        }
        
        private void createTestUsers() {
            try {
                // Create main test user with random email
                User testUser = new User();
                testUser.setEmail(testUserEmail);
                testUser.setPassword(passwordEncoder.encode(TestConstants.TEST_USER_PASSWORD));
                testUser.setPhoneNumber(TestConstants.TEST_USER_PHONE);
                testUser.setFirstName(TestConstants.TEST_USER_FIRST_NAME);
                testUser.setLastName(TestConstants.TEST_USER_LAST_NAME);
                testUser.setRole("USER");
                
                userRepository.save(testUser);
                System.out.println("✅ Created test user: " + testUser.getEmail());
            } catch (Exception e) {
                System.err.println("⚠️ Error creating test user: " + e.getMessage());
            }
        }
    }
} 