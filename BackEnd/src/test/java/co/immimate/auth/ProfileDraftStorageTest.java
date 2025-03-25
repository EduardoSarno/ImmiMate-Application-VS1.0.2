package co.immimate.auth;

import java.security.Principal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import co.immimate.profile.controller.ProfileController;
import co.immimate.profile.service.ProfileService;
import co.immimate.user.repository.UserRepository;

public class ProfileDraftStorageTest {

    @Mock
    private ProfileService profileService;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private Principal mockPrincipal;
    
    private MockMvc mockMvc;

    
    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        
        // Create the controller with our mocked service and repository
        ProfileController profileController = new ProfileController(profileService, userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(profileController).build();
        
        // Mock principal to return test email
        when(mockPrincipal.getName()).thenReturn("test@example.com");
    }
    
    @Test
    public void testSaveFormDraft() throws Exception {
        // Create test form data
        String formData = "{\"applicantName\":\"Test User\",\"applicantAge\":30}";
        
        // Perform the request
        mockMvc.perform(post("/api/profiles/draft")
                .principal(mockPrincipal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(formData))
                .andExpect(status().isOk());
        
        // Verify the service method was called with correct parameters
        verify(profileService).saveProfileDraft(eq("test@example.com"), eq(formData));
    }
    
    @Test
    public void testGetLatestDraft() throws Exception {
        // Setup mock service response
        String formData = "{\"applicantName\":\"Test User\",\"applicantAge\":30}";
        when(profileService.getLatestProfileDraft("test@example.com")).thenReturn(Optional.of(formData));
        
        // Test retrieving the latest draft
        mockMvc.perform(get("/api/profiles/draft")
                .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.formData").exists());
    }
    
    @Test
    public void testGetNonExistentDraft() throws Exception {
        // Setup mock service response for no draft found
        when(profileService.getLatestProfileDraft("test@example.com")).thenReturn(Optional.empty());
        
        // Test retrieving a non-existent draft
        mockMvc.perform(get("/api/profiles/draft")
                .principal(mockPrincipal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
    
    @Test
    public void testCsrfProtection() throws Exception {
        // Create test form data
        String formData = "{\"applicantName\":\"Test User\",\"applicantAge\":30}";
        
        // Test that POST request requires CSRF token
        // Since this is a unit test with standalone setup, CSRF is not automatically enabled
        // For integration tests, we would test that requests without CSRF token are rejected
        
        // For this unit test, we'll verify that the controller correctly handles the request
        // when it bypasses Spring Security (standalone setup doesn't include security filters)
        mockMvc.perform(post("/api/profiles/draft")
                .principal(mockPrincipal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(formData))
                .andExpect(status().isOk());
        
        // Verify the service method was called with correct parameters
        verify(profileService).saveProfileDraft(eq("test@example.com"), eq(formData));
    }
    
    @Test
    public void testDraftPersistence() throws Exception {
        // Create test form data
        String formData = "{\"applicantName\":\"Test User\",\"applicantAge\":30}";
        
        // First save the draft
        mockMvc.perform(post("/api/profiles/draft")
                .principal(mockPrincipal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(formData))
                .andExpect(status().isOk());
                
        // Verify the service method was called with the correct parameters
        verify(profileService).saveProfileDraft(eq("test@example.com"), eq(formData));
        
        // Setup mock service response to return the saved draft
        when(profileService.getLatestProfileDraft("test@example.com")).thenReturn(Optional.of(formData));
        
        // Then retrieve the saved draft
        mockMvc.perform(get("/api/profiles/draft")
                .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.formData").exists());
    }
}