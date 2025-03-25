package co.immimate.user;

import java.util.UUID;

import javax.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.immimate.auth.dto.LoginRequest;
import co.immimate.auth.dto.RegisterRequest;
import co.immimate.test.TestConfig;
import co.immimate.test.TestConstants;
import co.immimate.test.TestDataInitializer.TestDataService;
import co.immimate.user.model.User;
import co.immimate.user.repository.UserRepository;

/**
 * Integration tests for user-related functionality.
 * Tests user creation, authentication, and validation.
 */
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional // Ensures each test runs in a transaction that is rolled back
public class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserTester userTester;
    
    @Autowired
    private TestDataService testDataService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private User testUser;
    
    @BeforeEach
    public void setup() {
        // Initialize test data
        testDataService.initializeTestData();
        
        // Get the test user
        testUser = userTester.getTestUser();
        
        // Verify test setup was successful
        assertNotNull(testUser, "Test user should not be null");
        assertNotNull(testUser.getEmail(), "Test user email should not be null");
        System.out.println("✅ UserIntegrationTest using email: " + testUser.getEmail());
    }
    
    @Test
    @DisplayName("Should create a test user with custom details")
    public void shouldCreateCustomTestUser() {
        // Create a custom test user
        String email = "custom" + System.currentTimeMillis() + "@example.com";
        User customUser = userTester.createCustomTestUser(
                email,
                "CustomPassword123!",
                "+15551234567",
                "Custom",
                "User"
        );
        
        // Verify user was created
        assertNotNull(customUser, "Custom user should not be null");
        assertNotNull(customUser.getId(), "Custom user ID should not be null");
        assertEquals(email, customUser.getEmail(), "Custom user email should match");
        assertEquals("Custom", customUser.getFirstName(), "Custom user first name should match");
        assertEquals("USER", customUser.getRole(), "Custom user role should be USER");
        
        // Verify user exists in database
        assertTrue(userRepository.findByEmail(email).isPresent(), "Custom user should exist in database");
    }
    
    @Test
    @DisplayName("Should create a test admin user")
    public void shouldCreateTestAdminUser() {
        // Create an admin user
        User adminUser = userTester.createTestAdminUser();
        
        // Verify admin user was created
        assertNotNull(adminUser, "Admin user should not be null");
        assertNotNull(adminUser.getId(), "Admin user ID should not be null");
        assertEquals("admin@example.com", adminUser.getEmail(), "Admin user email should match");
        assertEquals("ADMIN", adminUser.getRole(), "Admin user role should be ADMIN");
        
        // Verify admin user exists in database
        assertTrue(userRepository.findByEmail("admin@example.com").isPresent(), "Admin user should exist in database");
    }
    
    @Test
    @DisplayName("Should authenticate user with valid credentials")
    public void shouldAuthenticateUserWithValidCredentials() throws Exception {
        // Create login request using the current test user's email, not the constant
        LoginRequest loginRequest = new LoginRequest(
                testUser.getEmail(), 
                TestConstants.TEST_USER_PASSWORD
        );
        
        // Perform login
        MvcResult result = mockMvc.perform(post(TestConstants.LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(testUser.getEmail()))
                .andReturn();
                
        // Verify JWT cookie is present
        Cookie jwtCookie = result.getResponse().getCookie("jwt");
        assertNotNull(jwtCookie, "JWT cookie should not be null");
        if (jwtCookie != null) {
            assertTrue(jwtCookie.isHttpOnly(), "JWT cookie should be HTTP-only");
        }
    }
    
    @Test
    @DisplayName("Should reject authentication with invalid credentials")
    public void shouldRejectAuthenticationWithInvalidCredentials() throws Exception {
        // Create login request with wrong password
        LoginRequest loginRequest = new LoginRequest(
                testUser.getEmail(), 
                "WrongPassword123!"
        );
        
        // Perform login
        mockMvc.perform(post(TestConstants.LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("Should reject authentication for non-existent user")
    public void shouldRejectAuthenticationForNonExistentUser() throws Exception {
        // Create login request with non-existent email
        String nonExistentEmail = "nonexistent" + UUID.randomUUID().toString() + "@example.com";
        LoginRequest loginRequest = new LoginRequest(
                nonExistentEmail, 
                "AnyPassword123!"
        );
        
        // Perform login
        mockMvc.perform(post(TestConstants.LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("Should register new user with valid details")
    public void shouldRegisterNewUserWithValidDetails() throws Exception {
        // Create unique email for registration
        String uniqueEmail = "newuser" + UUID.randomUUID().toString() + "@example.com";
        
        // Create registration request
        RegisterRequest registerRequest = new RegisterRequest(
                uniqueEmail,
                "ValidPassword123!",
                "New",
                "User",
                "+1" + (Math.abs(UUID.randomUUID().getMostSignificantBits()) % 10000000000L)
        );
        
        // Perform registration
        mockMvc.perform(post(TestConstants.REGISTER_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"));
        
        // Verify user exists in database
        assertTrue(userRepository.findByEmail(uniqueEmail).isPresent(), 
                "Newly registered user should exist in database");
    }
    
    @Test
    @DisplayName("Should reject registration with invalid email format")
    public void shouldRejectRegistrationWithInvalidEmailFormat() throws Exception {
        // Create registration request with invalid email
        RegisterRequest registerRequest = new RegisterRequest(
                "invalid-email",
                TestConstants.TEST_USER_PASSWORD,
                "Invalid",
                "User",
                "+15551234567"
        );
        
        // Perform registration
        mockMvc.perform(post(TestConstants.REGISTER_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should reject registration with invalid phone number format")
    public void shouldRejectRegistrationWithInvalidPhoneNumberFormat() throws Exception {
        // Create registration request with invalid phone
        RegisterRequest registerRequest = new RegisterRequest(
                "valid@example.com",
                TestConstants.TEST_USER_PASSWORD,
                "Invalid",
                "User",
                "InvalidPhoneNumber"
        );
        
        // Perform registration
        mockMvc.perform(post(TestConstants.REGISTER_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should reject registration with duplicate email")
    public void shouldRejectRegistrationWithDuplicateEmail() throws Exception {
        // Create registration request with existing email
        RegisterRequest registerRequest = new RegisterRequest(
                testUser.getEmail(),
                TestConstants.TEST_USER_PASSWORD,
                "Duplicate",
                "User",
                "+15559876543"
        );
        
        // Perform registration
        mockMvc.perform(post(TestConstants.REGISTER_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is already in use"));
    }
    
    @Test
    @DisplayName("Should allow access to protected endpoint with valid token")
    public void shouldAllowAccessToProtectedEndpointWithValidToken() throws Exception {
        // Login to get token
        LoginRequest loginRequest = new LoginRequest(
                testUser.getEmail(), 
                TestConstants.TEST_USER_PASSWORD
        );
        
        MvcResult result = mockMvc.perform(post(TestConstants.LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        // Extract JWT cookie from response
        Cookie jwtCookie = result.getResponse().getCookie("jwt");
        
        // Verify JWT cookie was obtained
        assertNotNull(jwtCookie, "JWT cookie should not be null");
        
        // Access protected endpoint with JWT cookie
        mockMvc.perform(get(TestConstants.ME_ENDPOINT)
                .cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(testUser.getEmail()));
    }
    
    @Test
    @DisplayName("Should reject access to protected endpoint without authentication")
    public void shouldRejectAccessToProtectedEndpointWithoutAuthentication() throws Exception {
        // Try to access protected endpoint without authentication
        mockMvc.perform(get(TestConstants.ME_ENDPOINT))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("Should reject registration with weak password")
    public void shouldRejectRegistrationWithWeakPassword() throws Exception {
        // Create registration request with weak password
        RegisterRequest registerRequest = new RegisterRequest(
                "newuser" + UUID.randomUUID().toString() + "@example.com",
                "weak",  // Too short, no uppercase, no numbers, no special chars
                "Weak",
                "Password",
                "+15551234567"
        );
        
        // Perform registration
        mockMvc.perform(post(TestConstants.REGISTER_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should reject registration with missing required fields")
    public void shouldRejectRegistrationWithMissingRequiredFields() throws Exception {
        // Create registration request with missing fields
        RegisterRequest registerRequest = new RegisterRequest(
                "newuser" + UUID.randomUUID().toString() + "@example.com",
                "ValidPassword123!",
                "",  // Empty first name
                "",  // Empty last name
                "+15551234567"
        );
        
        // Perform registration
        mockMvc.perform(post(TestConstants.REGISTER_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }
} 