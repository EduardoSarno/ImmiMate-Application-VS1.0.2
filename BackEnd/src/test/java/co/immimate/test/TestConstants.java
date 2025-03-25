package co.immimate.test;

import java.util.UUID;

/**
 * Constants used across test classes.
 * Centralizing test constants makes tests more maintainable and less prone to errors.
 */
public class TestConstants {
    
    // Base email for randomization
    private static final String EMAIL_BASE = "testuser";
    private static final String EMAIL_DOMAIN = "example.com";
    
    // Test user credentials - password and other details are still static
    public static final String TEST_USER_PASSWORD = "TestPassword123!";
    public static final String TEST_USER_PHONE = "+15551234567";
    public static final String TEST_USER_FIRST_NAME = "Test";
    public static final String TEST_USER_LAST_NAME = "User";
    
    // Password for new test users
    public static final String NEW_USER_PASSWORD = "Password123!";
    public static final String NEW_USER_PHONE = "+1234567890";
    public static final String NEW_USER_FIRST_NAME = "New";
    public static final String NEW_USER_LAST_NAME = "User";
    
    // Invalid credentials
    public static final String WRONG_PASSWORD = "WrongPassword";
    
    // API endpoints - using relative paths for test environment with random port
    public static final String AUTH_BASE_URL = "/api/auth";
    public static final String LOGIN_ENDPOINT = "/api/auth/login";
    public static final String REGISTER_ENDPOINT = "/api/auth/register";
    public static final String ME_ENDPOINT = "/api/auth/me";
    
    // Expected response messages
    public static final String REGISTRATION_SUCCESS_MESSAGE = "User registered successfully";
    public static final String EMAIL_IN_USE_MESSAGE = "Email is already in use";
    
    // Methods to generate random test emails
    private static String generateRandomSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Generates a random email for the primary test user
     * @return A random test email
     */
    public static String generateTestUserEmail() {
        return EMAIL_BASE + "_" + generateRandomSuffix() + "@" + EMAIL_DOMAIN;
    }
    
    /**
     * Generates a random email for new users in registration tests
     * @return A random email for new test users
     */
    public static String generateNewUserEmail() {
        return "newuser_" + generateRandomSuffix() + "@" + EMAIL_DOMAIN;
    }
    
    // For backward compatibility - these methods return a new random email each time
    // but can be used where the code expects the old constants
    public static String TEST_USER_EMAIL = generateTestUserEmail();
    public static String NEW_USER_EMAIL = generateNewUserEmail();
    
    // Prevent instantiation
    private TestConstants() {
        throw new AssertionError("Utility class should not be instantiated");
    }
} 