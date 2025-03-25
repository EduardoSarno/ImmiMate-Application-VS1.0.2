package co.immimate.profile.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import co.immimate.profile.dto.CLBConversionResponse;
import co.immimate.profile.model.CLBTestConverter;
import co.immimate.profile.repository.CLBTestConverterRepository;

@ExtendWith(MockitoExtension.class)
class CLBTestConverterServiceTest {

    @Mock
    private CLBTestConverterRepository repository;

    @InjectMocks
    private CLBTestConverterService service;

    private List<CLBTestConverter> mockData;

    @BeforeEach
    public void setUp() {
        // Create mock data based on TESTS_COVERTED_TO_CLB_2.0.csv
        mockData = new ArrayList<>();
        
        // CLB Level 10
        CLBTestConverter clb10 = new CLBTestConverter();
        clb10.setId(1L);
        clb10.setClbLevel(10);
        clb10.setCelpipListening("10");
        clb10.setCelpipReading("10");
        clb10.setCelpipWriting("10");
        clb10.setCelpipSpeaking("10");
        clb10.setIeltsListening("8.5");
        clb10.setIeltsReading("8.0");
        clb10.setIeltsWriting("7.5");
        clb10.setIeltsSpeaking("7.5");
        clb10.setPteListening("89-90");
        clb10.setPteReading("88-90");
        clb10.setPteWriting("90");
        clb10.setPteSpeaking("89-90");
        clb10.setTefListening("316-360");
        clb10.setTefReading("263-300");
        clb10.setTefWriting("393-450");
        clb10.setTefSpeaking("393-450");
        clb10.setTcfListening("549-699");
        clb10.setTcfReading("549-699");
        clb10.setTcfWriting("16-20");
        clb10.setTcfSpeaking("16-20");
        clb10.setLastUpdated(LocalDateTime.now());
        clb10.setActive(true);
        mockData.add(clb10);
        
        // CLB Level 9
        CLBTestConverter clb9 = new CLBTestConverter();
        clb9.setId(2L);
        clb9.setClbLevel(9);
        clb9.setCelpipListening("9");
        clb9.setCelpipReading("9");
        clb9.setCelpipWriting("9");
        clb9.setCelpipSpeaking("9");
        clb9.setIeltsListening("8.0");
        clb9.setIeltsReading("7.0");
        clb9.setIeltsWriting("7.0");
        clb9.setIeltsSpeaking("7.0");
        clb9.setPteListening("82-88");
        clb9.setPteReading("78-87");
        clb9.setPteWriting("88-89");
        clb9.setPteSpeaking("84-88");
        clb9.setTefListening("298-315");
        clb9.setTefReading("248-262");
        clb9.setTefWriting("371-392");
        clb9.setTefSpeaking("371-392");
        clb9.setTcfListening("523-548");
        clb9.setTcfReading("524-548");
        clb9.setTcfWriting("14-15");
        clb9.setTcfSpeaking("14-15");
        clb9.setLastUpdated(LocalDateTime.now());
        clb9.setActive(true);
        mockData.add(clb9);
        
        // CLB Level 7
        CLBTestConverter clb7 = new CLBTestConverter();
        clb7.setId(3L);
        clb7.setClbLevel(7);
        clb7.setCelpipListening("7");
        clb7.setCelpipReading("7");
        clb7.setCelpipWriting("7");
        clb7.setCelpipSpeaking("7");
        clb7.setIeltsListening("6.0");
        clb7.setIeltsReading("6.0");
        clb7.setIeltsWriting("6.0");
        clb7.setIeltsSpeaking("6.0");
        clb7.setPteListening("60-70");
        clb7.setPteReading("60-68");
        clb7.setPteWriting("69-78");
        clb7.setPteSpeaking("68-75");
        clb7.setTefListening("249-279");
        clb7.setTefReading("207-232");
        clb7.setTefWriting("310-348");
        clb7.setTefSpeaking("310-348");
        clb7.setTcfListening("458-502");
        clb7.setTcfReading("453-498");
        clb7.setTcfWriting("10-11");
        clb7.setTcfSpeaking("10-11");
        clb7.setLastUpdated(LocalDateTime.now());
        clb7.setActive(true);
        mockData.add(clb7);
        
        // CLB Level 4
        CLBTestConverter clb4 = new CLBTestConverter();
        clb4.setId(4L);
        clb4.setClbLevel(4);
        clb4.setCelpipListening("4");
        clb4.setCelpipReading("4");
        clb4.setCelpipWriting("4");
        clb4.setCelpipSpeaking("4");
        clb4.setIeltsListening("4.5");
        clb4.setIeltsReading("3.5");
        clb4.setIeltsWriting("4.0");
        clb4.setIeltsSpeaking("4.0");
        clb4.setPteListening("28-38");
        clb4.setPteReading("33-41");
        clb4.setPteWriting("41-50");
        clb4.setPteSpeaking("42-50");
        clb4.setTefListening("145-180");
        clb4.setTefReading("121-150");
        clb4.setTefWriting("181-225");
        clb4.setTefSpeaking("181-225");
        clb4.setTcfListening("331-368");
        clb4.setTcfReading("342-374");
        clb4.setTcfWriting("4-5");
        clb4.setTcfSpeaking("4-5");
        clb4.setLastUpdated(LocalDateTime.now());
        clb4.setActive(true);
        mockData.add(clb4);
    }

    @Test
    @DisplayName("Should retrieve all active conversions")
    void getAllConversions() {
        // Arrange
        when(repository.findByActiveIsTrueOrderByClbLevelDesc()).thenReturn(mockData);
        
        // Act
        List<CLBTestConverter> result = service.getAllConversions();
        
        // Assert
        assertNotNull(result);
        assertEquals(4, result.size());
        verify(repository, times(1)).findByActiveIsTrueOrderByClbLevelDesc();
    }
    
    @Test
    @DisplayName("Should get conversion tables correctly structured")
    void getConversionTables() {
        // Arrange
        when(repository.findByActiveIsTrueOrderByClbLevelDesc()).thenReturn(mockData);
        
        // Act
        CLBConversionResponse response = service.getConversionTables();
        
        // Assert
        assertNotNull(response);
        assertNotNull(response.getCelpip());
        assertNotNull(response.getIelts());
        assertNotNull(response.getPte());
        assertNotNull(response.getTef());
        assertNotNull(response.getTcf());
        
        // Verify structure for one specific test type and skill
        assertNotNull(response.getCelpip().get("listening"));
        assertNotNull(response.getIelts().get("reading"));
        
        // Verify correct CLB level mapping for a few specific scores
        assertEquals(10, response.getCelpip().get("listening").get("10"));
        assertEquals(9, response.getIelts().get("reading").get("7.0"));
        assertEquals(7, response.getPte().get("writing").get("69-78"));
    }
    
    @Test
    @DisplayName("Should convert CELPIP scores to CLB levels correctly")
    void convertCelpipToCLB() {
        // Arrange
        when(repository.findByActiveIsTrueOrderByClbLevelDesc()).thenReturn(mockData);
        
        // Act & Assert
        // Test for various CELPIP scores
        assertEquals(10, service.convertToCLB("CELPIP", "listening", "10"));
        assertEquals(9, service.convertToCLB("CELPIP", "reading", "9"));
        assertEquals(7, service.convertToCLB("CELPIP", "writing", "7"));
        assertEquals(4, service.convertToCLB("CELPIP", "speaking", "4"));
        
        // Test for invalid inputs
        assertNull(service.convertToCLB("CELPIP", "listening", "13")); // Invalid score
        assertNull(service.convertToCLB("CELPIP", "invalid", "7")); // Invalid skill
    }
    
    @Test
    @DisplayName("Should convert IELTS scores to CLB levels correctly")
    void convertIeltsToCLB() {
        // Arrange
        when(repository.findByActiveIsTrueOrderByClbLevelDesc()).thenReturn(mockData);
        
        // Act & Assert
        // Test for various IELTS scores
        assertEquals(10, service.convertToCLB("IELTS", "listening", "8.5"));
        assertEquals(9, service.convertToCLB("IELTS", "reading", "7.0"));
        assertEquals(7, service.convertToCLB("IELTS", "writing", "6.0"));
        assertEquals(4, service.convertToCLB("IELTS", "speaking", "4.0"));
        
        // Test for invalid inputs
        assertNull(service.convertToCLB("IELTS", "listening", "10.0")); // Invalid score
        assertNull(service.convertToCLB("IELTS", "invalid", "6.0")); // Invalid skill
    }
    
    @Test
    @DisplayName("Should convert PTE scores to CLB levels correctly")
    void convertPteToCLB() {
        // Arrange
        when(repository.findByActiveIsTrueOrderByClbLevelDesc()).thenReturn(mockData);
        
        // Act & Assert
        // Test for specific scores within ranges
        assertEquals(10, service.convertToCLB("PTE", "listening", "90"));
        assertEquals(9, service.convertToCLB("PTE", "reading", "85"));
        assertEquals(7, service.convertToCLB("PTE", "writing", "75"));
        assertEquals(4, service.convertToCLB("PTE", "speaking", "45"));
        
        // Test for invalid inputs
        assertNull(service.convertToCLB("PTE", "listening", "95")); // Invalid score
        assertNull(service.convertToCLB("PTE", "invalid", "65")); // Invalid skill
    }
    
    @Test
    @DisplayName("Should convert TEF scores to CLB levels correctly")
    void convertTefToCLB() {
        // Arrange
        when(repository.findByActiveIsTrueOrderByClbLevelDesc()).thenReturn(mockData);
        
        // Act & Assert
        // Test for specific scores within ranges
        assertEquals(10, service.convertToCLB("TEF", "listening", "320"));
        assertEquals(9, service.convertToCLB("TEF", "reading", "250"));
        assertEquals(7, service.convertToCLB("TEF", "writing", "330"));
        assertEquals(4, service.convertToCLB("TEF", "speaking", "200"));
        
        // Test for invalid inputs
        assertNull(service.convertToCLB("TEF", "listening", "500")); // Invalid score
        assertNull(service.convertToCLB("TEF", "invalid", "300")); // Invalid skill
    }
    
    @Test
    @DisplayName("Should convert TCF scores to CLB levels correctly")
    void convertTcfToCLB() {
        // Arrange
        when(repository.findByActiveIsTrueOrderByClbLevelDesc()).thenReturn(mockData);
        
        // Act & Assert
        // Test for specific scores within ranges
        assertEquals(10, service.convertToCLB("TCF", "listening", "600"));
        assertEquals(9, service.convertToCLB("TCF", "reading", "530"));
        assertEquals(7, service.convertToCLB("TCF", "writing", "10"));
        assertEquals(4, service.convertToCLB("TCF", "speaking", "5"));
        
        // Test for invalid inputs
        assertNull(service.convertToCLB("TCF", "listening", "1500")); // Invalid score
        assertNull(service.convertToCLB("TCF", "invalid", "500")); // Invalid skill
    }
    
    @Test
    @DisplayName("Should handle invalid or null inputs gracefully")
    void handleInvalidInputs() {
        // Arrange
        when(repository.findByActiveIsTrueOrderByClbLevelDesc()).thenReturn(mockData);
        
        // Act & Assert
        assertNull(service.convertToCLB("INVALID_TEST", "listening", "8.0")); // Invalid test type
        assertNull(service.convertToCLB(null, "reading", "7.0")); // Null test type
        assertNull(service.convertToCLB("IELTS", null, "6.0")); // Null skill
        assertNull(service.convertToCLB("IELTS", "writing", null)); // Null score
        assertNull(service.convertToCLB("PTE", "speaking", "not_a_number")); // Non-numeric score
    }
} 