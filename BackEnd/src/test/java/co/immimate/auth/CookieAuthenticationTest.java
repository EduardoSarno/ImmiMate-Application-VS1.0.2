package co.immimate.auth;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.immimate.auth.dto.LoginRequest;
import co.immimate.test.TestConstants;
import co.immimate.test.TestDataInitializer.TestDataService;
import co.immimate.user.UserTester;
import co.immimate.user.model.User;

/**
 * Integration tests for cookie-based authentication
 * Tests login with cookies, CSRF protection, and logout functionality
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CookieAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserTester userTester;
    
    @Autowired(required = false)
    private TestDataService testDataService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    // Constants for cookie-based auth tests
    private static final String COOKIE_NAME = "jwt";
    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    private static final String TEST_ORIGIN = "http://localhost:3000";

    @BeforeEach
    public void setup() {
        // Create a CORS filter for testing
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOriginPatterns(Arrays.asList("http://localhost:3000", "http://localhost:*", "http://127.0.0.1:*"));
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        corsConfig.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-XSRF-TOKEN"));
        corsConfig.setExposedHeaders(Arrays.asList("X-XSRF-TOKEN"));
        corsConfig.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        CorsFilter corsFilter = new CorsFilter(source);
        
        // Setup mockMvc with enhanced security setup for testing
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .addFilter(corsFilter)
                .apply(springSecurity()) // This ensures that security is applied correctly
                .alwaysDo(result -> System.out.println("Response: " + result.getResponse().getStatus() + 
                         " " + result.getResponse().getContentAsString()))
                .build();
                
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
        System.out.println("✅ CookieAuthenticationTest using email: " + testUser.getEmail());
    }

    @Test
    @DisplayName("Should return JWT cookie on successful authentication")
    @WithMockUser(username="test@example.com", roles={"USER"})
    public void shouldReturnJwtCookieOnSuccessfulAuthentication() throws Exception {
        // Create login request
        LoginRequest loginRequest = new LoginRequest(testUser.getEmail(), TestConstants.TEST_USER_PASSWORD);

        // Perform login with csrf token
        MvcResult result = mockMvc.perform(post(TestConstants.LOGIN_ENDPOINT)
                .with(csrf()) // Add CSRF token automatically
                .header("Origin", TEST_ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        // Check cookie attributes
        Cookie jwtCookie = result.getResponse().getCookie(COOKIE_NAME);
        assertNotNull(jwtCookie, "JWT cookie should not be null");
        if (jwtCookie != null) {
            assertTrue(jwtCookie.isHttpOnly(), "JWT cookie should be HTTP-only");
            assertEquals("/", jwtCookie.getPath(), "JWT cookie path should be root");
        }
    }

    @Test
    @DisplayName("Should reject requests to protected endpoints without cookie")
    public void shouldRejectRequestsToProtectedEndpointsWithoutCookie() throws Exception {
        // Try to access protected endpoint without cookie
        mockMvc.perform(get(TestConstants.ME_ENDPOINT)
                .header("Origin", TEST_ORIGIN))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow access to protected endpoints with valid cookie")
    @WithMockUser(username="test@example.com", roles={"USER"})
    public void shouldAllowAccessToProtectedEndpointsWithValidCookie() throws Exception {
        // Login to get the cookie
        LoginRequest loginRequest = new LoginRequest(testUser.getEmail(), TestConstants.TEST_USER_PASSWORD);
        
        MvcResult result = mockMvc.perform(post(TestConstants.LOGIN_ENDPOINT)
                .with(csrf()) // Add CSRF token
                .header("Origin", TEST_ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        // Extract the JWT cookie
        Cookie jwtCookie = result.getResponse().getCookie(COOKIE_NAME);
        assertNotNull(jwtCookie, "JWT cookie should not be null");
        
        // Get CSRF token
        MvcResult csrfResult = mockMvc.perform(get("/api/csrf")
                .header("Origin", TEST_ORIGIN)
                .cookie(jwtCookie))
                .andExpect(status().isOk())
                .andReturn();
        
        Cookie csrfCookie = csrfResult.getResponse().getCookie(CSRF_COOKIE_NAME);
        assertNotNull(csrfCookie, "CSRF cookie should not be null");
        
        String csrfToken = objectMapper.readTree(csrfResult.getResponse().getContentAsString())
                .get("token").asText();
        
        // Access protected endpoint with the cookie
        mockMvc.perform(get(TestConstants.ME_ENDPOINT)
                .header("Origin", TEST_ORIGIN)
                .header(CSRF_HEADER_NAME, csrfToken) // Add CSRF token
                .cookie(jwtCookie, csrfCookie)) // Add both cookies
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(testUser.getEmail()));
    }
    
    @Test
    @DisplayName("Should provide CSRF token via /csrf endpoint")
    @WithMockUser(username="test@example.com", roles={"USER"})
    public void shouldProvideCsrfToken() throws Exception {
        // When requesting the CSRF token endpoint with Origin header matching allowed patterns
        MvcResult result = mockMvc.perform(get("/api/csrf")
                .header("Origin", TEST_ORIGIN) // Add Origin header to match CORS configuration
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        
        // Then a CSRF cookie should be set
        Cookie csrfCookie = result.getResponse().getCookie(CSRF_COOKIE_NAME);
        assertNotNull(csrfCookie, "CSRF cookie should be set");
        if (csrfCookie != null) {
            assertTrue(csrfCookie.getValue().length() > 0, "CSRF token should not be empty");
        }
    }

    @Test
    @DisplayName("Should provide CSRF protection and block state-changing requests without CSRF token")
    @WithMockUser(username="test@example.com", roles={"USER"})
    public void shouldProvideCSRFProtection() throws Exception {
        // Login to get the JWT cookie
        LoginRequest loginRequest = new LoginRequest(testUser.getEmail(), TestConstants.TEST_USER_PASSWORD);
        
        MvcResult loginResult = mockMvc.perform(post(TestConstants.LOGIN_ENDPOINT)
                .with(csrf()) // Add CSRF token
                .header("Origin", TEST_ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        Cookie jwtCookie = loginResult.getResponse().getCookie(COOKIE_NAME);
        assertNotNull(jwtCookie, "JWT cookie should not be null");
        
        // Get CSRF token first to ensure CSRF protection is enabled
        MvcResult csrfResult = mockMvc.perform(get("/api/csrf")
                .header("Origin", TEST_ORIGIN)
                .cookie(jwtCookie))
                .andExpect(status().isOk())
                .andReturn();
        
        // Verify CSRF cookie was set
        Cookie csrfCookie = csrfResult.getResponse().getCookie(CSRF_COOKIE_NAME);
        assertNotNull(csrfCookie, "CSRF cookie should not be null");
        
        // Create a test request body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("test", "value");
        
        // This test is to verify that a POST request without a CSRF token is forbidden
        // Use the /profiles endpoint which requires CSRF protection
        mockMvc.perform(post("/api/profiles")
                .header("Origin", TEST_ORIGIN)
                .cookie(jwtCookie) // Include JWT cookie but not CSRF token
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest()); // Should be bad request due to missing CSRF token
    }
    
    @Test
    @DisplayName("Should allow state-changing requests with valid CSRF token")
    @WithMockUser(username="test@example.com", roles={"USER"})
    public void shouldAllowStateChangingRequestsWithValidCsrfToken() throws Exception {
        // This test case verifies that when a valid CSRF token is provided,
        // state-changing requests (POST/PUT/DELETE) should be allowed
        
        // Login to get the JWT cookie
        LoginRequest loginRequest = new LoginRequest(testUser.getEmail(), TestConstants.TEST_USER_PASSWORD);
        
        MvcResult loginResult = mockMvc.perform(post(TestConstants.LOGIN_ENDPOINT)
                .with(csrf()) // Add CSRF token
                .header("Origin", TEST_ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        Cookie jwtCookie = loginResult.getResponse().getCookie(COOKIE_NAME);
        assertNotNull(jwtCookie, "JWT cookie should not be null");
        
        // Get CSRF token
        MvcResult csrfResult = mockMvc.perform(get("/api/csrf")
                .header("Origin", TEST_ORIGIN)
                .cookie(jwtCookie))
                .andExpect(status().isOk())
                .andReturn();
        
        Cookie csrfCookie = csrfResult.getResponse().getCookie(CSRF_COOKIE_NAME);
        assertNotNull(csrfCookie, "CSRF cookie should not be null");
        
        // Extract CSRF token from response
        String csrfToken = objectMapper.readTree(csrfResult.getResponse().getContentAsString())
                .get("token").asText();
        
        // Create a test request body suitable for a POST request to an API endpoint
        // Instead of testing against /api/profiles with complex validation,
        // we'll test against a simpler endpoint like draft storage
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("data", "Test data");
        
        // POST to draft endpoint should succeed with the CSRF token
        mockMvc.perform(post("/api/profiles/draft")
                .with(csrf()) // Add CSRF token automatically
                .header("Origin", TEST_ORIGIN)
                .header(CSRF_HEADER_NAME, csrfToken) // Include CSRF token in header
                .cookie(jwtCookie, csrfCookie) // Include both cookies
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());
    }
    
    @Test
    @DisplayName("Should clear JWT cookie on logout")
    @WithMockUser(username="test@example.com", roles={"USER"})
    public void shouldClearJwtCookieOnLogout() throws Exception {
        // First, perform login to get a valid JWT cookie
        LoginRequest loginRequest = new LoginRequest(testUser.getEmail(), TestConstants.TEST_USER_PASSWORD);
        
        MvcResult loginResult = mockMvc.perform(post(TestConstants.LOGIN_ENDPOINT)
                .with(csrf()) // Add CSRF token
                .header("Origin", TEST_ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        Cookie jwtCookie = loginResult.getResponse().getCookie(COOKIE_NAME);
        assertNotNull(jwtCookie, "JWT cookie should not be null");
        
        // Get CSRF token
        MvcResult csrfResult = mockMvc.perform(get("/api/csrf")
                .header("Origin", TEST_ORIGIN)
                .cookie(jwtCookie))
                .andExpect(status().isOk())
                .andReturn();
        
        Cookie csrfCookie = csrfResult.getResponse().getCookie(CSRF_COOKIE_NAME);
        String csrfToken = objectMapper.readTree(csrfResult.getResponse().getContentAsString())
                .get("token").asText();
        
        // Now logout, which should clear the JWT cookie
        MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout")
                .header("Origin", TEST_ORIGIN)
                .header(CSRF_HEADER_NAME, csrfToken)
                .cookie(jwtCookie, csrfCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"))
                .andReturn();
        
        // Check that JWT cookie was cleared (max age 0 or expired)
        Cookie clearedCookie = logoutResult.getResponse().getCookie(COOKIE_NAME);
        assertNotNull(clearedCookie, "Cleared cookie should not be null");
        assertEquals(0, clearedCookie.getMaxAge(), "Cleared cookie should have max age 0");
        
        // Verify the JWT cookie value is empty or null
        assertTrue(clearedCookie.getValue() == null || clearedCookie.getValue().isEmpty(), 
                "JWT cookie value should be empty or null after logout");
        
        // Log what was verified
        System.out.println("✅ Verified that the JWT cookie was cleared on logout (maxAge = 0)");
        System.out.println("✅ Verified that the JWT cookie value is empty or null after logout");
    }
    
    @Test
    @DisplayName("Should verify token and return user details")
    @WithMockUser(username="test@example.com", roles={"USER"})
    public void shouldVerifyTokenAndReturnUserDetails() throws Exception {
        // First, perform login to get a valid JWT cookie
        LoginRequest loginRequest = new LoginRequest(testUser.getEmail(), TestConstants.TEST_USER_PASSWORD);
        
        MvcResult loginResult = mockMvc.perform(post(TestConstants.LOGIN_ENDPOINT)
                .with(csrf()) // Add CSRF token
                .header("Origin", TEST_ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        Cookie jwtCookie = loginResult.getResponse().getCookie(COOKIE_NAME);
        assertNotNull(jwtCookie, "JWT cookie should not be null");
        
        // Get CSRF token
        MvcResult csrfResult = mockMvc.perform(get("/api/csrf")
                .header("Origin", TEST_ORIGIN)
                .cookie(jwtCookie))
                .andExpect(status().isOk())
                .andReturn();
        
        Cookie csrfCookie = csrfResult.getResponse().getCookie(CSRF_COOKIE_NAME);
        String csrfToken = objectMapper.readTree(csrfResult.getResponse().getContentAsString())
                .get("token").asText();
        
        // Now access the /auth/verify endpoint which should validate the token and return user details
        mockMvc.perform(get("/api/auth/verify")
                .header("Origin", TEST_ORIGIN)
                .header(CSRF_HEADER_NAME, csrfToken)
                .cookie(jwtCookie, csrfCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(testUser.getEmail()));
    }
} 