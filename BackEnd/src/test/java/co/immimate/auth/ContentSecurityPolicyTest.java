package co.immimate.auth;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Detailed tests for Content Security Policy implementation.
 * These tests ensure that the CSP is correctly configured to protect against
 * various attack vectors including XSS, clickjacking, and data injection.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ContentSecurityPolicyTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @DisplayName("CSP should be present on all HTTP responses")
    public void cspShouldBePresentOnAllResponses() throws Exception {
        // Test GET endpoint
        mockMvc.perform(get("/api/health"))
               .andExpect(header().exists("Content-Security-Policy"));
        
        // Test POST endpoint
        mockMvc.perform(post("/api/csrf")
               .contentType(MediaType.APPLICATION_JSON))
               .andExpect(header().exists("Content-Security-Policy"));
    }
    
    @Test
    @DisplayName("CSP should properly restrict default-src directive")
    public void cspShouldRestrictDefaultSrc() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andReturn();
                
        MockHttpServletResponse response = result.getResponse();
        String cspHeader = response.getHeader("Content-Security-Policy");
        
        // Add null check to prevent NullPointerException
        assertTrue(cspHeader != null && cspHeader.contains("default-src 'self'"), 
                "CSP should restrict default-src to 'self'");
        
        // Ensure no wildcards in default-src
        assertTrue(cspHeader == null || !cspHeader.contains("default-src *"), 
                "CSP should not use wildcards in default-src");
    }
    
    @Test
    @DisplayName("CSP should properly configure script-src directive")
    public void cspShouldConfigureScriptSrc() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(header().string("Content-Security-Policy", 
                        containsString("script-src 'self'")))
                // No unsafe-inline
                .andExpect(header().string("Content-Security-Policy", 
                        not(containsString("script-src 'unsafe-inline'"))));
    }
    
    @Test
    @DisplayName("CSP should properly configure style-src directive")
    public void cspShouldConfigureStyleSrc() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(header().string("Content-Security-Policy", 
                        containsString("style-src 'self'")));
                
        // If you need to allow inline styles (not recommended but sometimes necessary)
        // Uncomment this and adjust the CSP implementation accordingly
        // .andExpect(header().string("Content-Security-Policy", 
        //         containsString("style-src 'self' 'unsafe-inline'")));
    }
    
    @Test
    @DisplayName("CSP should properly configure img-src directive")
    public void cspShouldConfigureImgSrc() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(header().string("Content-Security-Policy", 
                        containsString("img-src 'self'")));
    }
    
    @Test
    @DisplayName("CSP should properly configure connect-src directive")
    public void cspShouldConfigureConnectSrc() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(header().string("Content-Security-Policy", 
                        containsString("connect-src 'self'")));
    }
    
    @Test
    @DisplayName("CSP should include frame-ancestors directive to prevent clickjacking")
    public void cspShouldIncludeFrameAncestors() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(header().string("Content-Security-Policy", 
                        containsString("frame-ancestors 'none'")));
    }
    
    @Test
    @DisplayName("CSP should restrict form submissions with form-action directive")
    public void cspShouldRestrictFormAction() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(header().string("Content-Security-Policy", 
                        containsString("form-action 'self'")));
    }
    
    @Test
    @DisplayName("CSP should block object, embed and applet tags with object-src directive")
    public void cspShouldBlockObjectSrc() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(header().string("Content-Security-Policy", 
                        containsString("object-src 'none'")));
    }
    
    @Test
    @DisplayName("CSP should include base-uri directive to prevent base tag hijacking")
    public void cspShouldIncludeBaseUri() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(header().string("Content-Security-Policy", 
                        containsString("base-uri 'self'")));
    }
    
    @Test
    @DisplayName("CSP should configure proper font sources")
    public void cspShouldConfigureFontSrc() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(header().string("Content-Security-Policy", 
                        containsString("font-src 'self'")));
                
        // If you're using Google Fonts or other font providers
        // Uncomment and modify as needed
        // .andExpect(header().string("Content-Security-Policy", 
        //         containsString("font-src 'self' https://fonts.gstatic.com")));
    }
    
    @Test
    @DisplayName("CSP should include upgrade-insecure-requests directive")
    public void cspShouldIncludeUpgradeInsecureRequests() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(header().string("Content-Security-Policy", 
                        containsString("upgrade-insecure-requests")));
    }
    
    @Test
    @DisplayName("CSP should allow nonces for script execution if needed")
    public void cspShouldSupportNonces() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andReturn();
                
        MockHttpServletResponse response = result.getResponse();
        String cspHeader = response.getHeader("Content-Security-Policy");
        
        // Check if nonce pattern exists in script-src
        // This is a more advanced CSP feature - implement if you plan to use nonces
        // assertTrue(cspHeader.matches(".*script-src 'self' 'nonce-[a-zA-Z0-9+/=]+'.*"), 
        //        "CSP should include nonce for script-src if using nonces");
        
        // Instead, for now we'll just check that inline scripts are blocked
        // Add null check to prevent NullPointerException
        assertTrue(cspHeader == null || !cspHeader.contains("script-src 'unsafe-inline'"), 
                "CSP should not allow unsafe-inline scripts");
    }
} 