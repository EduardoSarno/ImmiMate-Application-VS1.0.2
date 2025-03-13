package co.immimate.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import co.immimate.test.TestConstants;
import co.immimate.test.TestDataInitializer.TestDataService;
import co.immimate.user.UserTester;
import co.immimate.user.model.User;

/**
 * Integration tests for the authentication system
 * Tests user registration, login, and protected endpoints
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, args = {"co.immimate.auth", "co.immimate.user"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional // This ensures each test runs in a transaction that is rolled back after the test completes
public class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserTester userTester;
    
    @Autowired(required = false)
    private TestDataService testDataService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    @BeforeEach
    public void setup() {
        // Initialize test data
        if (testDataService != null) {
            testDataService.initializeTestData();
        } else {
            // Fallback to using UserTester directly
            userTester.createTestUser();
        }
        
        // Get the test user
        testUser = userTester.getTestUser();
        
        // Verify test setup was successful
        assertNotNull(testUser, "Test user should not be null");
        assertNotNull(testUser.getEmail(), "Test user email should not be null");
        assertEquals(TestConstants.TEST_USER_EMAIL, testUser.getEmail(), "Test user email should match expected value");
    }

    @Test
    @DisplayName("Should successfully authenticate user with valid credentials")
    public void shouldAuthenticateUserWithValidCredentials() throws Exception {
        // Create login request using the raw password from TestConstants, not the encoded password from the user object
        LoginRequest loginRequest = new LoginRequest(testUser.getEmail(), TestConstants.TEST_USER_PASSWORD);

        // Perform login
        mockMvc.perform(post(TestConstants.LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("Should reject authentication with invalid credentials")
    public void shouldRejectAuthenticationWithInvalidCredentials() throws Exception {
        // Create login request with wrong password
        LoginRequest loginRequest = new LoginRequest(testUser.getEmail(), TestConstants.WRONG_PASSWORD);

        // Perform login
        mockMvc.perform(post(TestConstants.LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should successfully register new user")
    public void shouldRegisterNewUser() throws Exception {
        // Create registration request
        RegisterRequest registerRequest = new RegisterRequest(
                TestConstants.NEW_USER_EMAIL,
                TestConstants.NEW_USER_PASSWORD,
                TestConstants.NEW_USER_FIRST_NAME,
                TestConstants.NEW_USER_LAST_NAME,
                TestConstants.NEW_USER_PHONE
        );

        // Perform registration
        mockMvc.perform(post(TestConstants.REGISTER_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(TestConstants.REGISTRATION_SUCCESS_MESSAGE));
    }

    @Test
    @DisplayName("Should reject registration with duplicate email")
    public void shouldRejectRegistrationWithDuplicateEmail() throws Exception {
        // Create registration request with existing email
        RegisterRequest registerRequest = new RegisterRequest(
                testUser.getEmail(),
                TestConstants.NEW_USER_PASSWORD,
                "Duplicate",
                "User",
                TestConstants.NEW_USER_PHONE
        );

        // Perform registration
        mockMvc.perform(post(TestConstants.REGISTER_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(TestConstants.EMAIL_IN_USE_MESSAGE));
    }

    @Test
    @DisplayName("Should allow access to protected endpoint with valid token")
    public void shouldAllowAccessToProtectedEndpointWithValidToken() throws Exception {
        // Login to get token - use raw password from TestConstants
        LoginRequest loginRequest = new LoginRequest(testUser.getEmail(), TestConstants.TEST_USER_PASSWORD);
        
        MvcResult result = mockMvc.perform(post(TestConstants.LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        // Extract token from response
        String responseJson = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseJson).get("token").asText();
        
        // Verify token was obtained
        assertNotNull(token, "JWT token should not be null");
        
        // Access protected endpoint with token
        mockMvc.perform(get(TestConstants.ME_ENDPOINT)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("Should reject access to protected endpoint without authentication")
    public void shouldRejectAccessToProtectedEndpointWithoutAuthentication() throws Exception {
        // Try to access protected endpoint without authentication
        mockMvc.perform(get(TestConstants.ME_ENDPOINT))
                .andExpect(status().isUnauthorized());
    }
} 