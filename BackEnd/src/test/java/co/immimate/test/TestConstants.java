package co.immimate.test;

/**
 * Constants used across test classes.
 * Centralizing test constants makes tests more maintainable and less prone to errors.
 */
public class TestConstants {
    
    // Test user credentials
    public static final String TEST_USER_EMAIL = "testuser@example.com";
    public static final String TEST_USER_PASSWORD = "TestPassword123!";
    public static final String TEST_USER_PHONE = "+15551234567";
    public static final String TEST_USER_FIRST_NAME = "Test";
    public static final String TEST_USER_LAST_NAME = "User";
    
    // New test user for registration tests
    public static final String NEW_USER_EMAIL = "newuser@immimate.co";
    public static final String NEW_USER_PASSWORD = "Password123!";
    public static final String NEW_USER_PHONE = "+1234567890";
    public static final String NEW_USER_FIRST_NAME = "New";
    public static final String NEW_USER_LAST_NAME = "User";
    
    // Invalid credentials
    public static final String WRONG_PASSWORD = "WrongPassword";
    
    // API endpoints - using relative paths for test environment with random port
    public static final String AUTH_BASE_URL = "/api/auth";  // Changed to relative URL
    public static final String LOGIN_ENDPOINT = "/auth/login";
    public static final String REGISTER_ENDPOINT = "/auth/register";
    public static final String ME_ENDPOINT = "/auth/me";
    
    // Expected response messages
    public static final String REGISTRATION_SUCCESS_MESSAGE = "User registered successfully";
    public static final String EMAIL_IN_USE_MESSAGE = "Email is already in use";
    
    // Prevent instantiation
    private TestConstants() {
        throw new AssertionError("Utility class should not be instantiated");
    }
} 