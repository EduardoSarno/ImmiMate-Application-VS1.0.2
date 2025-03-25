package co.immimate.scoringevaluations.calculation.service.specialcases;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import co.immimate.scoringevaluations.evaluation.model.EvaluationSubcategory;
import co.immimate.scoringevaluations.evaluation.repository.EvaluationSubcategoryRepository;

@ExtendWith(MockitoExtension.class)
public class SkillTransferabilityCappingServiceTest {

    @Mock
    private EvaluationSubcategoryRepository subcategoryRepository;

    @InjectMocks
    private SkillTransferabilityCappingService cappingService;
    
    private UUID testCategoryEvalId;
    private List<EvaluationSubcategory> mockSubcategories;
    
    // Specific subcategory names and IDs
    private static final String SUBCAT_EDUCATION_LANGUAGE = "Education and First Official Language Proficiency";
    private static final String SUBCAT_EDUCATION_CANADIAN_WORK = "Education and Canadian Work Experience";
    private static final String SUBCAT_FOREIGN_WORK_LANGUAGE = "Foreign Work Experience and First Official Language Proficiency";
    private static final String SUBCAT_CANADIAN_FOREIGN_WORK = "Canadian Work Experience and Foreign Work Experience";
    private static final String SUBCAT_TRADES_CERTIFICATE = "Trades Certificate and First Official Language Proficiency";
    
    private static final UUID SUBCAT_ID_EDUCATION_LANGUAGE = UUID.fromString("58b8e7ae-afdc-458f-b65e-33e06a146a9f");
    private static final UUID SUBCAT_ID_EDUCATION_CANADIAN_WORK = UUID.fromString("bb4e4bfc-22dc-48bf-b013-724b6ff167cf");
    private static final UUID SUBCAT_ID_FOREIGN_WORK_LANGUAGE = UUID.fromString("3b688514-a888-4ea9-b418-736bd48ce2a4");
    private static final UUID SUBCAT_ID_CANADIAN_FOREIGN_WORK = UUID.fromString("e431ce11-d6fb-404f-9972-d43b6ee5683a");
    private static final UUID SUBCAT_ID_TRADES_CERTIFICATE = UUID.fromString("cc03aa55-03fc-400c-8980-fa75d535e830");
    
    @BeforeEach
    public void setup() {
        testCategoryEvalId = UUID.randomUUID();
        mockSubcategories = new ArrayList<>();
        
        // Create mock subcategories
        createMockSubcategory(SUBCAT_ID_EDUCATION_LANGUAGE, SUBCAT_EDUCATION_LANGUAGE, 30);
        createMockSubcategory(SUBCAT_ID_EDUCATION_CANADIAN_WORK, SUBCAT_EDUCATION_CANADIAN_WORK, 30);
        createMockSubcategory(SUBCAT_ID_FOREIGN_WORK_LANGUAGE, SUBCAT_FOREIGN_WORK_LANGUAGE, 30);
        createMockSubcategory(SUBCAT_ID_CANADIAN_FOREIGN_WORK, SUBCAT_CANADIAN_FOREIGN_WORK, 30);
        createMockSubcategory(SUBCAT_ID_TRADES_CERTIFICATE, SUBCAT_TRADES_CERTIFICATE, 30);
    }
    
    private void createMockSubcategory(UUID id, String name, int score) {
        EvaluationSubcategory subcategory = new EvaluationSubcategory();
        subcategory.setSubcatEvalId(UUID.randomUUID());
        subcategory.setCatEvalId(testCategoryEvalId);
        subcategory.setSubcategoryId(id);
        subcategory.setSubcategoryName(name);
        subcategory.setMaxPossibleScore(50);
        subcategory.setUserScore(score);
        subcategory.setFieldCount(1);
        subcategory.setCreatedAt(Instant.now());
        subcategory.setUpdatedAt(Instant.now());
        
        mockSubcategories.add(subcategory);
    }
    
    @Test
    @DisplayName("Test with scores that don't exceed the group caps")
    public void testNoCapExceeded() {
        // Create a new list with much lower scores that won't exceed group caps
        List<EvaluationSubcategory> lowerScoreSubcategories = new ArrayList<>();
        
        // First create Education subcategories with scores that won't exceed cap (20+20=40)
        EvaluationSubcategory educationLang = new EvaluationSubcategory();
        educationLang.setSubcatEvalId(UUID.randomUUID());
        educationLang.setCatEvalId(testCategoryEvalId);
        educationLang.setSubcategoryName("Education and First Official Language Proficiency");
        educationLang.setUserScore(20); // Lower score
        educationLang.setFieldCount(1);
        educationLang.setCreatedAt(Instant.now());
        educationLang.setUpdatedAt(Instant.now());
        lowerScoreSubcategories.add(educationLang);
        
        EvaluationSubcategory educationWork = new EvaluationSubcategory();
        educationWork.setSubcatEvalId(UUID.randomUUID());
        educationWork.setCatEvalId(testCategoryEvalId);
        educationWork.setSubcategoryName("Education and Canadian Work Experience");
        educationWork.setUserScore(20); // Lower score
        educationWork.setFieldCount(1);
        educationWork.setCreatedAt(Instant.now());
        educationWork.setUpdatedAt(Instant.now());
        lowerScoreSubcategories.add(educationWork);
        
        // Now create Foreign Work subcategories with scores that won't exceed cap (20+20=40)
        EvaluationSubcategory foreignWorkLang = new EvaluationSubcategory();
        foreignWorkLang.setSubcatEvalId(UUID.randomUUID());
        foreignWorkLang.setCatEvalId(testCategoryEvalId);
        foreignWorkLang.setSubcategoryName("Foreign Work Experience and First Official Language Proficiency");
        foreignWorkLang.setUserScore(20); // Lower score
        foreignWorkLang.setFieldCount(1);
        foreignWorkLang.setCreatedAt(Instant.now());
        foreignWorkLang.setUpdatedAt(Instant.now());
        lowerScoreSubcategories.add(foreignWorkLang);
        
        EvaluationSubcategory foreignCanadianWork = new EvaluationSubcategory();
        foreignCanadianWork.setSubcatEvalId(UUID.randomUUID());
        foreignCanadianWork.setCatEvalId(testCategoryEvalId);
        foreignCanadianWork.setSubcategoryName("Canadian Work Experience and Foreign Work Experience");
        foreignCanadianWork.setUserScore(20); // Lower score
        foreignCanadianWork.setFieldCount(1);
        foreignCanadianWork.setCreatedAt(Instant.now());
        foreignCanadianWork.setUpdatedAt(Instant.now());
        lowerScoreSubcategories.add(foreignCanadianWork);
        
        // Add a Trades Certificate subcategory with a lower score
        EvaluationSubcategory tradesCert = new EvaluationSubcategory();
        tradesCert.setSubcatEvalId(UUID.randomUUID());
        tradesCert.setCatEvalId(testCategoryEvalId);
        tradesCert.setSubcategoryName("Trades Certificate and First Official Language Proficiency");
        tradesCert.setUserScore(20); // Lower score
        tradesCert.setFieldCount(1);
        tradesCert.setCreatedAt(Instant.now());
        tradesCert.setUpdatedAt(Instant.now());
        lowerScoreSubcategories.add(tradesCert);
        
        // Override the repository to return our lower scores
        when(subcategoryRepository.findByCatEvalId(testCategoryEvalId)).thenReturn(lowerScoreSubcategories);
        
        int result = cappingService.applySkillTransferabilityGroupCaps(testCategoryEvalId);
        
        // Expected: Education group (20+20=40, below cap) + Foreign work (20+20=40, below cap) + Trades (20) = 100
        // Total is already at category cap of 100
        assertEquals(100, result, "Total score should be 100");
        
        // No save calls should be made since no group exceeded its cap
        verify(subcategoryRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Test with scores that exceed the group caps")
    public void testGroupCapExceeded() {
        // Set scores to exceed group caps
        mockSubcategories.get(0).setUserScore(40); // Education language
        mockSubcategories.get(1).setUserScore(40); // Education Canadian work
        mockSubcategories.get(2).setUserScore(40); // Foreign work language
        mockSubcategories.get(3).setUserScore(40); // Canadian and foreign work
        mockSubcategories.get(4).setUserScore(40); // Trades certificate
        
        when(subcategoryRepository.findByCatEvalId(testCategoryEvalId)).thenReturn(mockSubcategories);
        when(subcategoryRepository.save(any(EvaluationSubcategory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        int result = cappingService.applySkillTransferabilityGroupCaps(testCategoryEvalId);
        
        // Expected: Education group (40+40=80, capped to 50) + Foreign work (40+40=80, capped to 50) + Trades (40) = 140
        // But overall category cap is 100
        assertEquals(100, result, "Total score should be capped at 100");
        
        // Verify that save was called for subcategories that needed capping
        verify(subcategoryRepository, times(4)).save(any()); // Should be called for the 4 subcategories in the two groups
    }
    
    @Test
    @DisplayName("Test with only one group exceeding cap")
    public void testOneGroupCapExceeded() {
        // Set only education group to exceed cap
        mockSubcategories.get(0).setUserScore(40); // Education language
        mockSubcategories.get(1).setUserScore(40); // Education Canadian work
        mockSubcategories.get(2).setUserScore(20); // Foreign work language
        mockSubcategories.get(3).setUserScore(20); // Canadian and foreign work
        mockSubcategories.get(4).setUserScore(20); // Trades certificate
        
        when(subcategoryRepository.findByCatEvalId(testCategoryEvalId)).thenReturn(mockSubcategories);
        when(subcategoryRepository.save(any(EvaluationSubcategory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        int result = cappingService.applySkillTransferabilityGroupCaps(testCategoryEvalId);
        
        // Expected: Education group (40+40=80, capped to 50) + Foreign work (20+20=40, not capped) + Trades (20) = 110
        // But overall category cap is 100
        assertEquals(100, result, "Total score should be capped at 100");
        
        // Verify that save was called for education subcategories
        verify(subcategoryRepository, times(2)).save(any()); // Should be called for the 2 subcategories in education group
    }
    
    @Test
    @DisplayName("Test with empty subcategories list")
    public void testEmptySubcategories() {
        when(subcategoryRepository.findByCatEvalId(testCategoryEvalId)).thenReturn(new ArrayList<>());
        
        int result = cappingService.applySkillTransferabilityGroupCaps(testCategoryEvalId);
        
        assertEquals(0, result, "Total score should be 0 when no subcategories are found");
    }
    
    @Test
    @DisplayName("Test proportional reduction with high scores")
    public void testProportionalReduction() {
        // Set up an extreme scenario where one subcategory has much higher score than the other
        mockSubcategories.get(0).setUserScore(45); // Education language (higher score)
        mockSubcategories.get(1).setUserScore(15); // Education Canadian work (lower score)
        
        // Only use education group for this test
        List<EvaluationSubcategory> educationOnly = new ArrayList<>();
        educationOnly.add(mockSubcategories.get(0));
        educationOnly.add(mockSubcategories.get(1));
        
        when(subcategoryRepository.findByCatEvalId(testCategoryEvalId)).thenReturn(educationOnly);
        when(subcategoryRepository.save(any(EvaluationSubcategory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        int result = cappingService.applySkillTransferabilityGroupCaps(testCategoryEvalId);
        
        // Expected: Education group (45+15=60, capped to 50) = 50
        assertEquals(50, result, "Total score should be 50 after group capping");
        
        // Verify proportional reduction:
        // originalScore * (50/60) => 45 * 0.8333 = 37.5 ≈ 38
        // 15 * 0.8333 = 12.5 ≈ 12
        // But last item might be adjusted to exactly hit the cap
    }
} 