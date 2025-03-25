package co.immimate.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import co.immimate.auth.dto.AuthResponse;
import co.immimate.auth.dto.LoginRequest;
import co.immimate.auth.dto.RegisterRequest;
import co.immimate.test.TestConstants;
import co.immimate.user.model.User;
import co.immimate.user.repository.UserRepository;

/**
 * Comprehensive utility class for testing user-related functionality.
 * Provides methods for testing user creation, authentication, and validation.
 * Works with the TestDataInitializer to ensure consistent test data.
 */
@Profile("test")
@Component
@Transactional
public class UserTester {

    private static final String BASE_URL = TestConstants.AUTH_BASE_URL;
    
    // Store the current session's test email
    private final String testUserEmail;
    private User testUser;
    private String testUserJwtToken;

    @Autowired(required = false)
    private RestTemplate restTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * Constructor that initializes a unique test email for this instance
     */
    public UserTester() {
        // Generate a unique test email for this instance of UserTester
        this.testUserEmail = TestConstants.generateTestUserEmail();
        System.out.println("⚡ Initialized UserTester with email: " + testUserEmail);
    }

    /**
     * Creates a test user with the default test credentials.
     * This method is now primarily used by the TestDataService.
     * 
     * @return The created test user
     */
    public User createTestUser() {
        // First check if the test user already exists to prevent duplicates
        Optional<User> existingUser = userRepository.findByEmail(testUserEmail);
        if (existingUser.isPresent()) {
            System.out.println("✅ Test user already exists, returning existing user: " + existingUser.get().getEmail());
            return existingUser.get();
        }
        
        // Create new user if not found
        System.out.println("✅ Creating new test user: " + testUserEmail);
        User newUser = new User();
        newUser.setEmail(testUserEmail);
        newUser.setPassword(passwordEncoder.encode(TestConstants.TEST_USER_PASSWORD));
        newUser.setPhoneNumber(TestConstants.TEST_USER_PHONE);
        newUser.setFirstName(TestConstants.TEST_USER_FIRST_NAME);
        newUser.setLastName(TestConstants.TEST_USER_LAST_NAME);
        newUser.setRole("USER");
        
        return userRepository.save(newUser);
    }
    
    /**
     * Creates a test user with custom details.
     * Useful for creating multiple test users with different attributes.
     * 
     * @param email User email
     * @param password Raw password (will be encoded)
     * @param phone Phone number
     * @param firstName First name
     * @param lastName Last name
     * @return The created user
     */
    public User createCustomTestUser(String email, String password, String phone, String firstName, String lastName) {
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setPhoneNumber(phone);
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setRole("USER");
        
        return userRepository.save(newUser);
    }
    
    /**
     * Creates a test admin user.
     * 
     * @return The created admin user
     */
    public User createTestAdminUser() {
        User adminUser = new User();
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(passwordEncoder.encode("AdminPassword123!"));
        adminUser.setPhoneNumber("+19876543210");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setRole("ADMIN");
        
        return userRepository.save(adminUser);
    }

    /**
     * Gets the test user, creating it if it doesn't exist.
     * 
     * @return The test user
     */
    public User getTestUser() {
        // Always check the database first in case the user was deleted
        Optional<User> existingUser = userRepository.findByEmail(testUserEmail);
        
        if (existingUser.isPresent()) {
            // Update the cached user
            testUser = existingUser.get();
            System.out.println("✅ Retrieved existing test user: " + testUser.getEmail());
        } else {
            // Create the user if it doesn't exist
            testUser = createTestUser();
            System.out.println("✅ Created new test user: " + testUser.getEmail());
        }
        
        return testUser;
    }

    /**
     * Authenticates the test user via the API and retrieves a JWT token.
     * 
     * @return The JWT token
     */
    @SuppressWarnings("null")
    public String authenticateTestUser() {
        User user = getTestUser(); // Ensure test user exists
        LoginRequest loginRequest = new LoginRequest(user.getEmail(), TestConstants.TEST_USER_PASSWORD);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                BASE_URL + "/login",
                loginRequest,
                AuthResponse.class
        );

        if (response.getStatusCodeValue() == 200 && response.getBody() != null) {
            testUserJwtToken = response.getBody().getToken();
            return testUserJwtToken;
        } else {
            throw new RuntimeException("Failed to authenticate test user: " + response.getStatusCodeValue());
        }
    }
    
    /**
     * Authenticates a user with custom credentials.
     * 
     * @param email User email
     * @param password Raw password
     * @return The JWT token if authentication is successful, null otherwise
     */
    @SuppressWarnings("null")
    public String authenticateUser(String email, String password) {
        LoginRequest loginRequest = new LoginRequest(email, password);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                BASE_URL + "/login",
                loginRequest,
                AuthResponse.class
        );

        if (response.getStatusCodeValue() == 200 && response.getBody() != null) {
            return response.getBody().getToken();
        }
        return null;
    }

    /**
     * Tests authentication with invalid credentials.
     * 
     * @return true if authentication properly fails, false otherwise
     */
    public boolean testInvalidAuthentication() {
        LoginRequest loginRequest = new LoginRequest(testUserEmail, "WrongPassword123!");

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                BASE_URL + "/login",
                loginRequest,
                AuthResponse.class
        );

        return response.getStatusCodeValue() == 401;
    }
    
    /**
     * Tests authentication with a non-existent user.
     * 
     * @return true if authentication properly fails, false otherwise
     */
    public boolean testNonExistentUserAuthentication() {
        String nonExistentEmail = "nonexistent" + UUID.randomUUID().toString() + "@example.com";
        LoginRequest loginRequest = new LoginRequest(nonExistentEmail, "AnyPassword123!");

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                BASE_URL + "/login",
                loginRequest,
                AuthResponse.class
        );

        return response.getStatusCodeValue() == 401;
    }

    /**
     * Tests user registration with valid details.
     * 
     * @return true if registration is successful, false otherwise
     */
    public boolean testValidRegistration() {
        String uniqueEmail = "newuser" + UUID.randomUUID().toString() + "@example.com";
        
        RegisterRequest registerRequest = new RegisterRequest(
                uniqueEmail,
                "ValidPassword123!",
                "New",
                "User",
                "+1" + (Math.abs(UUID.randomUUID().getMostSignificantBits()) % 10000000000L)
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                BASE_URL + "/register",
                registerRequest,
                String.class
        );

        return response.getStatusCodeValue() == 200;
    }

    /**
     * Tests registration with an invalid email format.
     * 
     * @return true if registration properly fails, false otherwise
     */
    public boolean testInvalidEmailRegistration() {
        RegisterRequest registerRequest = new RegisterRequest(
                "invalid-email",
                TestConstants.TEST_USER_PASSWORD,
                "Invalid",
                "User",
                "+15551234567"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                BASE_URL + "/register",
                registerRequest,
                String.class
        );

        return response.getStatusCodeValue() == 400;
    }

    /**
     * Tests registration with an invalid phone number format.
     * 
     * @return true if registration properly fails, false otherwise
     */
    public boolean testInvalidPhoneNumberRegistration() {
        RegisterRequest registerRequest = new RegisterRequest(
                "valid@example.com",
                TestConstants.TEST_USER_PASSWORD,
                "Invalid",
                "User",
                "InvalidPhoneNumber"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                BASE_URL + "/register",
                registerRequest,
                String.class
        );

        return response.getStatusCodeValue() == 400;
    }

    /**
     * Tests registration with a duplicate email.
     * 
     * @return true if registration properly fails, false otherwise
     */
    public boolean testDuplicateEmailRegistration() {
        // Ensure test user exists
        User existingUser = getTestUser();
        
        RegisterRequest registerRequest = new RegisterRequest(
                existingUser.getEmail(),
                TestConstants.TEST_USER_PASSWORD,
                "Duplicate",
                "User",
                "+15559876543"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                BASE_URL + "/register",
                registerRequest,
                String.class
        );

        return response.getStatusCodeValue() == 400;
    }

    /**
     * Tests accessing a protected endpoint with a valid token.
     * 
     * @return true if access is granted, false otherwise
     */
    public boolean testProtectedEndpointAccess() {
        // Ensure we have a token
        if (testUserJwtToken == null) {
            authenticateTestUser();
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + testUserJwtToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
                TestConstants.ME_ENDPOINT,
                HttpMethod.GET,
                entity,
                String.class
        );
        
        return response.getStatusCodeValue() == 200;
    }

    /**
     * Tests accessing a protected endpoint without authentication.
     * 
     * @return true if access is properly denied, false otherwise
     */
    public boolean testUnauthenticatedAccess() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                TestConstants.ME_ENDPOINT,
                String.class
        );
        
        return response.getStatusCodeValue() == 401;
    }

    /**
     * Returns the JWT token of the test user.
     */
    public String getUserJwtToken() {
        if (testUserJwtToken == null) {
            authenticateTestUser();
        }
        return testUserJwtToken;
    }

    /**
     * Returns the email being used for the test user in this session
     */
    public String getTestUserEmail() {
        return testUserEmail;
    }
}