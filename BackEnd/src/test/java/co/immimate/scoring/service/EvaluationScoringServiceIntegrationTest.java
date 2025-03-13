package co.immimate.scoring.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import co.immimate.profile.model.UserImmigrationProfile;
import co.immimate.profile.repository.ProfileRepository;
import co.immimate.scoring.fields.GridField;
import co.immimate.scoring.fields.GridFieldRepository;
import co.immimate.scoring.model.Evaluation;
import io.github.cdimascio.dotenv.Dotenv;


@SpringBootTest
@ActiveProfiles("test")
class EvaluationScoringServiceIntegrationTest {
    

    @Autowired
    private EvaluationScoringService evaluationScoringService;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private GridFieldRepository gridFieldRepository;

    private UUID testApplicationId;
    private final String testGridName = "Comprehensive Ranking System (CRS)"; // Ensure this exists in your DB

    /**
     * Load environment variables from .env in the project root.
     * This ensures database credentials are set before running tests.
     */
    @BeforeAll
    static void loadEnvVariables() {
        Dotenv dotenv = Dotenv.configure()
                .directory(System.getProperty("user.dir")) // Load from project root, not "BackEnd/BackEnd"
                .load();

        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        System.out.println("Loaded environment variables for testing.");
    }

    @BeforeEach
    void setUp() {
        // Ensure there is at least one test profile in the database
        Optional<UserImmigrationProfile> testProfile = profileRepository.findAll().stream().findFirst();

        if (testProfile.isEmpty()) {
            fail("No test profile found in the database. Please insert at least one test profile before running the tests.");
        }

        testApplicationId = testProfile.get().getApplicationId();
    }

    @Test
    void testPrintApplicationId() 
    {
    Optional<UserImmigrationProfile> testProfile = profileRepository.findAll().stream().findFirst();

    if (testProfile.isEmpty()) {
        System.out.println("No user profile found in the database.");
    } else {
        UUID applicationId = testProfile.get().getApplicationId();
        System.out.println("Retrieved Application ID: " + applicationId);
    }

    assertTrue(testProfile.isPresent(), "At least one user profile should exist in the database.");
}
    @Test
    void testEvaluateProfile_UsingDatabase() {
        // Fetch grid fields from the database
        List<GridField> gridFieldList = gridFieldRepository.findAllByGridName(testGridName);
        ArrayList<GridField> gridFields = new ArrayList<>(gridFieldList);

        assertFalse(gridFields.isEmpty(), "Grid fields should not be empty for a valid grid.");

        // Perform evaluation (WITHOUT saving to DB)
        Evaluation evaluation = evaluationScoringService.evaluateProfile(testApplicationId, testGridName);

        // Assertions
        assertNotNull(evaluation, "Evaluation result should not be null");
        assertEquals(testApplicationId, evaluation.getApplicationId(), "Application ID should match");
        assertEquals(testGridName, evaluation.getGridName(), "Grid name should match");
        assertTrue(evaluation.getTotalScore() >= 0, "Total score should be a valid non-negative number");
    }
}