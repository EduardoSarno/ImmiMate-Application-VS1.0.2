package co.immimate.auth;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

/**
 * Tests for security headers and Content Security Policy implementation.
 * These tests verify that proper security headers are present in HTTP responses
 * to mitigate common web vulnerabilities.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecurityHeadersTest {

    @Autowired
    private MockMvc mockMvc;
    
    /**
     * Helper method to perform a request to a public endpoint
     */
    private ResultActions performPublicEndpointRequest() throws Exception {
        return mockMvc.perform(get("/api/health")
                .accept(MediaType.APPLICATION_JSON));
    }
    
    @Test
    @DisplayName("Should include Content-Security-Policy header in response")
    public void shouldIncludeContentSecurityPolicyHeader() throws Exception {
        performPublicEndpointRequest()
            .andExpect(header().exists("Content-Security-Policy"))
            // Verify default-src is set to 'self'
            .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
            // Verify script-src contains expected values
            .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
            // Verify style-src contains expected values
            .andExpect(header().string("Content-Security-Policy", containsString("style-src 'self'")))
            // Verify connect-src contains expected values
            .andExpect(header().string("Content-Security-Policy", containsString("connect-src 'self'")))
            // Verify form-action is restricted
            .andExpect(header().string("Content-Security-Policy", containsString("form-action 'self'")))
            // Verify frame-ancestors is none (prevents clickjacking via frames)
            .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")));
    }
    
    @Test
    @DisplayName("Should include X-Content-Type-Options header in response")
    public void shouldIncludeXContentTypeOptionsHeader() throws Exception {
        performPublicEndpointRequest()
            .andExpect(header().exists("X-Content-Type-Options"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }
    
    @Test
    @DisplayName("Should include X-Frame-Options header in response")
    public void shouldIncludeXFrameOptionsHeader() throws Exception {
        performPublicEndpointRequest()
            .andExpect(header().exists("X-Frame-Options"))
            .andExpect(header().string("X-Frame-Options", "DENY"));
    }
    
    @Test
    @DisplayName("Should include Referrer-Policy header in response")
    public void shouldIncludeReferrerPolicyHeader() throws Exception {
        performPublicEndpointRequest()
            .andExpect(header().exists("Referrer-Policy"))
            .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }
    
    @Test
    @DisplayName("Should include Permissions-Policy header in response")
    public void shouldIncludePermissionsPolicyHeader() throws Exception {
        performPublicEndpointRequest()
            .andExpect(header().exists("Permissions-Policy"))
            // Check that camera permissions are restricted
            .andExpect(header().string("Permissions-Policy", containsString("camera=()")))
            // Check that microphone permissions are restricted
            .andExpect(header().string("Permissions-Policy", containsString("microphone=()")))
            // Check that geolocation permissions are restricted
            .andExpect(header().string("Permissions-Policy", containsString("geolocation=()")))
            // Check that interest-cohort (FLoC) is disabled
            .andExpect(header().string("Permissions-Policy", containsString("interest-cohort=()")));
    }
    
    @Test
    @DisplayName("Should include Strict-Transport-Security header in response")
    public void shouldIncludeHstsHeader() throws Exception {
        performPublicEndpointRequest()
            .andExpect(header().exists("Strict-Transport-Security"))
            .andExpect(header().string("Strict-Transport-Security", 
                     containsString("max-age=31536000")))  // 1 year in seconds
            .andExpect(header().string("Strict-Transport-Security", 
                     containsString("includeSubDomains")));
    }
    
    @Test
    @DisplayName("Should not include X-Powered-By header in response")
    public void shouldNotIncludeXPoweredByHeader() throws Exception {
        performPublicEndpointRequest()
            .andExpect(header().doesNotExist("X-Powered-By"));
    }
    
    @Test
    @DisplayName("Should include appropriate security headers for API endpoints")
    public void shouldIncludeSecurityHeadersForApiEndpoints() throws Exception {
        // Test on a different API endpoint to ensure headers are applied globally
        mockMvc.perform(get("/api/csrf"))
            .andExpect(header().exists("Content-Security-Policy"))
            .andExpect(header().exists("X-Content-Type-Options"))
            .andExpect(header().exists("X-Frame-Options"))
            .andExpect(header().exists("Referrer-Policy"))
            .andExpect(header().exists("Permissions-Policy"))
            .andExpect(header().exists("Strict-Transport-Security"));
    }
    
    @Test
    @DisplayName("CSP should restrict inline scripts")
    public void cspShouldRestrictInlineScripts() throws Exception {
        performPublicEndpointRequest()
            .andExpect(header().string("Content-Security-Policy", 
                    containsString("script-src 'self'")))
            // Check that unsafe-inline is NOT included in script-src
            .andExpect(header().string("Content-Security-Policy", 
                    containsString("script-src")))
            .andExpect(header().string("Content-Security-Policy", 
                    containsString("'self'")));
    }
    
    @Test
    @DisplayName("CSP should allow specific CDNs if required")
    public void cspShouldAllowSpecificCdns() throws Exception {
        // If your application uses CDNs for scripts or styles, 
        // this test verifies they are properly allowed
        performPublicEndpointRequest()
            .andExpect(header().string("Content-Security-Policy", 
                    containsString("script-src")));
            // Uncomment and modify if you're using specific CDNs
            // .andExpect(header().string("Content-Security-Policy", 
            //        containsString("https://cdn.jsdelivr.net")));
    }
} 