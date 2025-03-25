package co.immimate.auth;

import static org.junit.jupiter.api.Assertions.assertTrue;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import co.immimate.auth.dto.LoginRequest;

/**
 * Tests for rate limiting functionality.
 * These tests verify that rate limiting is properly applied to auth endpoints
 * to prevent brute force attacks.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RateLimitingTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final String LOGIN_ENDPOINT = "/api/auth/login";
    private static final String TEST_EMAIL = "ratelimit-test@example.com";
    private static final String TEST_PASSWORD = "invalid-password";
    private static final int RATE_LIMIT_MAX_ATTEMPTS = 5;
    
    // Header to explicitly enable rate limiting in tests
    private static final String TEST_RATE_LIMIT_HEADER = "X-Test-Rate-Limit";
    
    /**
     * Creates a login request with test credentials
     */
    private String createLoginRequest() throws Exception {
        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        return objectMapper.writeValueAsString(loginRequest);
    }
    
    @Test
    @DisplayName("Should allow limited number of login attempts")
    public void shouldAllowLimitedLoginAttempts() throws Exception {
        String loginRequestJson = createLoginRequest();
        
        // Make a series of requests up to the limit (should all be allowed)
        for (int i = 0; i < RATE_LIMIT_MAX_ATTEMPTS; i++) {
            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(TEST_RATE_LIMIT_HEADER, "true")
                    .content(loginRequestJson))
                    .andExpect(status().isUnauthorized()); // Expect 401 as credentials are invalid
        }
        
        // The next request should be rate limited
        MvcResult result = mockMvc.perform(post(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .header(TEST_RATE_LIMIT_HEADER, "true")
                .content(loginRequestJson))
                .andExpect(status().isTooManyRequests()) // Expect 429 Too Many Requests
                .andReturn();
        
        // Verify the response contains error message about rate limiting
        String responseContent = result.getResponse().getContentAsString();
        assertTrue(responseContent.contains("Too many requests"), 
                "Response should contain error message about rate limiting");
    }
    
    @Test
    @DisplayName("Should rate limit based on client IP address")
    public void shouldRateLimitBasedOnIpAddress() throws Exception {
        String loginRequestJson = createLoginRequest();
        
        // Make requests from "IP 1" until rate limited
        for (int i = 0; i < RATE_LIMIT_MAX_ATTEMPTS; i++) {
            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequestJson)
                    .header("X-Forwarded-For", "192.168.1.1")
                    .header(TEST_RATE_LIMIT_HEADER, "true"))
                    .andExpect(status().isUnauthorized());
        }
        
        // The next request from "IP 1" should be rate limited
        mockMvc.perform(post(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestJson)
                .header("X-Forwarded-For", "192.168.1.1")
                .header(TEST_RATE_LIMIT_HEADER, "true"))
                .andExpect(status().isTooManyRequests());
        
        // But a request from a different IP should be allowed
        mockMvc.perform(post(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestJson)
                .header("X-Forwarded-For", "192.168.1.2")
                .header(TEST_RATE_LIMIT_HEADER, "true"))
                .andExpect(status().isUnauthorized()); // Still get 401, not rate limited
    }
    
    @Test
    @DisplayName("Should enforce rate limits only on protected endpoints")
    public void shouldLimitOnlyProtectedEndpoints() throws Exception {
        String loginRequestJson = createLoginRequest();
        
        // First exhaust the rate limit on login endpoint
        for (int i = 0; i < RATE_LIMIT_MAX_ATTEMPTS; i++) {
            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequestJson)
                    .header("X-Forwarded-For", "192.168.1.3")
                    .header(TEST_RATE_LIMIT_HEADER, "true"))
                    .andExpect(status().isUnauthorized());
        }
        
        // Verify we're now rate limited on the login endpoint
        mockMvc.perform(post(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestJson)
                .header("X-Forwarded-For", "192.168.1.3")
                .header(TEST_RATE_LIMIT_HEADER, "true"))
                .andExpect(status().isTooManyRequests());
        
        // But we should still be able to access non-rate-limited endpoints
        mockMvc.perform(get("/api/csrf")
                .header("X-Forwarded-For", "192.168.1.3")
                .header(TEST_RATE_LIMIT_HEADER, "true"))
                .andExpect(status().isOk());
    }
    
    @Test
    @DisplayName("Rate limiting should return proper error response")
    public void shouldReturnProperErrorResponse() throws Exception {
        String loginRequestJson = createLoginRequest();
        
        // Exhaust the rate limit
        for (int i = 0; i < RATE_LIMIT_MAX_ATTEMPTS; i++) {
            mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequestJson)
                    .header("X-Forwarded-For", "192.168.1.4")
                    .header(TEST_RATE_LIMIT_HEADER, "true"))
                    .andExpect(status().isUnauthorized());
        }
        
        // Check the error response format
        mockMvc.perform(post(LOGIN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestJson)
                .header("X-Forwarded-For", "192.168.1.4")
                .header(TEST_RATE_LIMIT_HEADER, "true"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.status").value(429));
    }
} 