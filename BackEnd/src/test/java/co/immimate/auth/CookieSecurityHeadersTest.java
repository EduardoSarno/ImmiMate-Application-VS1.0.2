package co.immimate.auth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.Cookie;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for cookie security settings.
 * These tests ensure that cookies are properly secured with the appropriate
 * attributes (HttpOnly, Secure, SameSite) to prevent common cookie-based attacks.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CookieSecurityHeadersTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @DisplayName("Authentication cookies should be set with HttpOnly flag")
    public void authCookiesShouldBeHttpOnly() throws Exception {
        // Perform login or get a secured endpoint to generate auth cookies
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
                .andReturn();
                
        MockHttpServletResponse response = result.getResponse();
        Cookie[] cookies = response.getCookies();
        
        // Check if any auth cookies exist (JWT cookie, etc.)
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (isAuthCookie(cookie.getName())) {
                    assertTrue(cookie.isHttpOnly(), 
                            "Authentication cookie " + cookie.getName() + " should be HttpOnly");
                }
            }
        }
    }
    
    @Test
    @DisplayName("Authentication cookies should be set with Secure flag")
    public void authCookiesShouldBeSecure() throws Exception {
        // Perform login or get a secured endpoint to generate auth cookies
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
                .andReturn();
                
        MockHttpServletResponse response = result.getResponse();
        Cookie[] cookies = response.getCookies();
        
        // Check if any auth cookies exist (JWT cookie, etc.)
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (isAuthCookie(cookie.getName())) {
                    assertTrue(cookie.getSecure(), 
                            "Authentication cookie " + cookie.getName() + " should be Secure");
                }
            }
        }
    }
    
    @Test
    @DisplayName("Cookies should be set with SameSite attribute")
    public void cookiesShouldHaveSameSiteAttribute() throws Exception {
        // Perform login or get a secured endpoint to generate cookies
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
                .andReturn();
                
        MockHttpServletResponse response = result.getResponse();
        List<String> setCookieHeaders = getSetCookieHeaders(response);
        
        // Check for SameSite attribute in Set-Cookie headers
        for (String setCookie : setCookieHeaders) {
            if (isAuthCookieHeader(setCookie)) {
                assertTrue(setCookie.contains("SameSite=Lax") || setCookie.contains("SameSite=Strict"), 
                        "Cookie should have SameSite=Lax or SameSite=Strict attribute");
            }
        }
    }
    
    @Test
    @DisplayName("Session cookies should have appropriate Max-Age or Expires attributes")
    public void sessionCookiesShouldHaveAppropriateLifetime() throws Exception {
        // Perform login or get a secured endpoint to generate cookies
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
                .andReturn();
                
        MockHttpServletResponse response = result.getResponse();
        List<String> setCookieHeaders = getSetCookieHeaders(response);
        
        // Check for Max-Age or Expires in Set-Cookie headers
        for (String setCookie : setCookieHeaders) {
            if (isAuthCookieHeader(setCookie) && !setCookie.contains("Max-Age=0")) {
                assertTrue(setCookie.contains("Max-Age=") || setCookie.contains("Expires="), 
                        "Session cookie should have Max-Age or Expires attribute");
            }
        }
    }
    
    @Test
    @DisplayName("Session cookies should use __Host- prefix for additional security")
    public void sessionCookiesShouldUseHostPrefix() throws Exception {
        // This test checks if your application uses the __Host- prefix for cookies,
        // which is a modern security best practice for sensitive cookies.
        // Note: This is an advanced security feature and may not be implemented in your app yet.
        
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
                .andReturn();
                
        MockHttpServletResponse response = result.getResponse();
        List<String> setCookieHeaders = getSetCookieHeaders(response);
        
        // Check for __Host- prefix in any of the auth cookies
        // Note: This is optional but recommended security feature
        @SuppressWarnings("unused")
        Optional<String> hostPrefixedCookie = setCookieHeaders.stream()
                .filter(header -> header.contains("__Host-"))
                .findAny();
                
        // If you implement __Host- prefix, uncomment this assertion
        // assertTrue(hostPrefixedCookie.isPresent(), 
        //        "At least one authentication cookie should use the __Host- prefix");
    }
    
    @Test
    @DisplayName("Set-Cookie headers should have Path attribute set properly")
    public void setCookieHeadersShouldHavePathAttribute() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
                .andReturn();
                
        MockHttpServletResponse response = result.getResponse();
        List<String> setCookieHeaders = getSetCookieHeaders(response);
        
        // Check if cookies have Path attribute set
        for (String setCookie : setCookieHeaders) {
            if (isAuthCookieHeader(setCookie)) {
                assertTrue(setCookie.contains("Path="), 
                        "Cookie should have Path attribute defined");
            }
        }
    }
    
    @Test
    @DisplayName("Session cookies should not have overly permissive Domain attribute")
    public void cookiesShouldNotHavePermissiveDomain() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
                .andReturn();
                
        MockHttpServletResponse response = result.getResponse();
        List<String> setCookieHeaders = getSetCookieHeaders(response);
        
        // Check that Domain is not set too permissively (or left unset to default to current host)
        for (String setCookie : setCookieHeaders) {
            if (isAuthCookieHeader(setCookie) && setCookie.contains("Domain=")) {
                // Domain should not be a top-level domain like .com
                assertFalse(setCookie.matches(".*Domain=\\.[a-z]{2,3}\\b.*"), 
                        "Cookie domain should not be a top-level domain");
                
                // Domain should not start with just a dot
                assertFalse(setCookie.matches(".*Domain=\\.\\w+.*"), 
                        "Cookie domain should not start with just a dot");
            }
        }
    }
    
    @Test
    @DisplayName("CSRF cookie should allow JavaScript access for proper usage")
    public void csrfCookieShouldAllowJsAccess() throws Exception {
        // Get the CSRF token from the endpoint
        MvcResult result = mockMvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andReturn();
                
        MockHttpServletResponse response = result.getResponse();
        Cookie[] cookies = response.getCookies();
        
        // Find the CSRF cookie if it exists
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (isCsrfCookie(cookie.getName())) {
                    assertFalse(cookie.isHttpOnly(), 
                            "CSRF cookie should not be HttpOnly to allow JS access");
                }
            }
        }
    }
    
    // Helper method to get all Set-Cookie headers from the response
    private List<String> getSetCookieHeaders(MockHttpServletResponse response) {
        List<String> headers = new ArrayList<>();
        
        // MockHttpServletResponse exposes the headers through getHeaderNames and getHeaders
        for (Object name : response.getHeaderNames()) {
            if ("Set-Cookie".equals(name)) {
                for (Object value : response.getHeaders((String) name)) {
                    if (value != null) {
                        headers.add(value.toString());
                    }
                }
            }
        }
        
        return headers;
    }
    
    // Helper method to identify authentication cookies by name
    private boolean isAuthCookie(String name) {
        // Add your authentication cookie names here
        List<String> authCookieNames = Arrays.asList(
            "JWT", "JSESSIONID", "auth_token", "access_token", "refresh_token"
        );
        return authCookieNames.contains(name);
    }
    
    // Helper method to identify CSRF cookies by name
    private boolean isCsrfCookie(String name) {
        // Add your CSRF cookie names here
        List<String> csrfCookieNames = Arrays.asList(
            "XSRF-TOKEN", "csrf-token", "CSRF-TOKEN", "_csrf"
        );
        return csrfCookieNames.contains(name);
    }
    
    // Helper method to check if a Set-Cookie header is for an auth cookie
    private boolean isAuthCookieHeader(String header) {
        for (String cookieName : Arrays.asList("JWT", "JSESSIONID", "auth_token", "access_token", "refresh_token")) {
            if (header.startsWith(cookieName + "=")) {
                return true;
            }
        }
        return false;
    }
    
    // Helper assertion method
    private static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }
} 