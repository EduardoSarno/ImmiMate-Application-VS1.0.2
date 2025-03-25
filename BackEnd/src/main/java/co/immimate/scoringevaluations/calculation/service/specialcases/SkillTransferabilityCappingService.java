package co.immimate.scoringevaluations.calculation.service.specialcases;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.immimate.scoringevaluations.evaluation.model.EvaluationSubcategory;
import co.immimate.scoringevaluations.evaluation.repository.EvaluationSubcategoryRepository;

/**
 * Service dedicated to handling special capping cases for Skill Transferability factors.
 * The Skill Transferability category has a unique three-level capping system:
 * 1. Individual subcategory caps (each subcategory has a maximum of 50 points)
 * 2. Mid-level grouping caps (three functional groups with specific caps)
 * 3. Overall category cap (100 points total for Skill Transferability)
 */
@Service
public class SkillTransferabilityCappingService {

    private static final Logger logger = LoggerFactory.getLogger(SkillTransferabilityCappingService.class);
    
    // Constants for group names (matching the official CRS documentation)
    private static final String GROUP_EDUCATION = "Education";
    private static final String GROUP_FOREIGN_WORK = "Foreign work experience";
    
    // Maximum points for each group (regardless of subcategories)
    private static final int MAX_POINTS_PER_GROUP = 50;
    private static final int MAX_CATEGORY_POINTS = 100;
    private static final int MIN_SCORE = 0;
    

    
    // Log message constants
    private static final String LOG_PREFIX = "[SKILL TRANSFERABILITY] ";
    private static final String LOG_NO_SUBCATEGORIES = "No subcategories found for Skill Transferability category: {}";
    private static final String LOG_APPLYING_CAPPING = "Applying Skill Transferability special capping to category: {}";
    private static final String LOG_GROUP_EXCEEDS_CAP = LOG_PREFIX + "Group '{}' score ({}) exceeds cap of {} - applying group cap";
    private static final String LOG_GROUP_CONTRIBUTING = LOG_PREFIX + "Group '{}' contributing {} points to total";
    private static final String LOG_TRADES_CONTRIBUTING = LOG_PREFIX + "Trades Certificate contributing {} points to total";
    private static final String LOG_SCORE_EXCEEDS_CAP = LOG_PREFIX + "Total score after group capping ({}) exceeds category cap of {}";
    private static final String LOG_FINAL_SCORE = LOG_PREFIX + "Final score after all capping rules: {}";
    private static final String LOG_MAPPED_SUBCATEGORY = "Mapped subcategory '{}' to {} group";
    private static final String LOG_SKIPPING_TRADES = "Skipping Trades Certificate '{}' from group mapping (will be handled separately)";
    private static final String LOG_ADJUSTED_LAST_SUBCATEGORY = "Adjusted last subcategory '{}' score from {} to {} to ensure exact cap";
    private static final String LOG_PROPORTIONAL_ADJUSTMENT = "Proportionally adjusted subcategory '{}' score from {} to {}";
    
   
    private static final String FORMAT_GROUP_CAPPED = "Group '%s' capped from %d to %d points.";

    private static final String FORMAT_CATEGORY_CAPPING = "CATEGORY CAPPING APPLIED: Total reduced from %d to %d points\n";

    private static final String FORMAT_ADJUSTMENT_RESULT = "    - %s: %d → %d points\n";
    private static final String FORMAT_LAST_ADJUSTMENT = "    - %s: %d → %d points (adjusted to ensure exact cap)\n";
    

    
    
    @Autowired
    private EvaluationSubcategoryRepository subcategoryRepository;
    
    /**
     * Represents detailed information about the capping that was applied
     */
    public static class CappingDetails {
        private final StringBuilder shortNotes = new StringBuilder();
        private final StringBuilder detailedNotes = new StringBuilder();
        private int rawTotalScore = 0;
        private int cappedTotalScore = 0;
        // Maps to store raw and capped scores for groups
        private final Map<String, Integer> groupRawScores = new HashMap<>();
        private final Map<String, Integer> groupCappedScores = new HashMap<>();
        // List of subcategory names that were adjusted during capping
        private final List<String> subcategoryAdjustments = new ArrayList<>();
        
        public String getShortNotes() {
            return shortNotes.toString();
        }
        
        public String getDetailedNotes() {
            return detailedNotes.toString();
        }
        
        public boolean hasCapping() {
            return !subcategoryAdjustments.isEmpty() || rawTotalScore != cappedTotalScore;
        }
        
        /**
         * Adds a note about a capping event to the short notes.
         * 
         * @param note The capping note to add
         */
        public void addShortNote(String note) {
            if (shortNotes.length() > 0) {
                shortNotes.append(" ");
            }
            shortNotes.append(note);
        }
        
        /**
         * Adds a detailed note to the detailed notes section.
         * 
         * @param note The detailed note to add
         */
        public void addDetailedNote(String note) {
            detailedNotes.append(note);
        }
        
        /**
         * Records a capping adjustment for a subcategory.
         * 
         * @param subcategoryName The name of the subcategory
         */
        public void recordSubcategoryAdjustment(String subcategoryName) {
            subcategoryAdjustments.add(subcategoryName);
        }
        
        /**
         * Records score information for a group.
         * 
         * @param groupName The name of the group
         * @param rawScore The raw score before capping
         * @param cappedScore The capped score after applying limits
         */
        public void recordGroupScores(String groupName, int rawScore, int cappedScore) {
            groupRawScores.put(groupName, rawScore);
            groupCappedScores.put(groupName, cappedScore);
        }
        
        /**
         * Records overall score information.
         * 
         * @param rawScore The raw total score before capping
         * @param cappedScore The capped total score after applying limits
         */
        public void recordTotalScores(int rawScore, int cappedScore) {
            this.rawTotalScore = rawScore;
            this.cappedTotalScore = cappedScore;
        }
        
        /**
         * Returns the list of subcategories that were adjusted during capping.
         * @return List of subcategory names
         */
        public List<String> getAdjustedSubcategories() {
            return new ArrayList<>(subcategoryAdjustments);
        }
        
        /**
         * Returns the map of raw group scores before capping.
         * @return Map of group name to raw score
         */
        public Map<String, Integer> getGroupRawScores() {
            return new HashMap<>(groupRawScores);
        }
        
        /**
         * Returns the map of capped group scores after capping.
         * @return Map of group name to capped score
         */
        public Map<String, Integer> getGroupCappedScores() {
            return new HashMap<>(groupCappedScores);
        }
    }
    
    /**
     * Applies group caps to the Skill Transferability subcategories, ensuring:
     * 1. No group score exceeds 50 points
     * 2. Total Skill Transferability score does not exceed 100 points
     *
     * @param catEvalId The category evaluation ID
     * @return The capped final score for the category
     */
    public int applySkillTransferabilityGroupCaps(UUID catEvalId) {
        return applySkillTransferabilityGroupCaps(catEvalId, null);
    }

    /**
     * Applies group caps to the Skill Transferability subcategories, ensuring:
     * 1. No group score exceeds 50 points
     * 2. Total Skill Transferability score does not exceed 100 points
     *
     * Also collects capping details if a CappingDetails object is provided.
     *
     * @param catEvalId The category evaluation ID
     * @param cappingDetails Optional object to collect capping details
     * @return The capped final score for the category
     */
    public int applySkillTransferabilityGroupCaps(UUID catEvalId, CappingDetails cappingDetails) {
        logger.info(LOG_APPLYING_CAPPING, catEvalId);
        
        List<EvaluationSubcategory> subcategories = subcategoryRepository.findByCatEvalId(catEvalId);
        
        if (subcategories == null || subcategories.isEmpty()) {
            logger.warn(LOG_NO_SUBCATEGORIES, catEvalId);
            return MIN_SCORE;
        }
        
        // Map subcategories to their mid-level groups
        Map<String, List<EvaluationSubcategory>> groupsMap = new HashMap<>();
        List<EvaluationSubcategory> tradesCertificates = new ArrayList<>();
        
        for (EvaluationSubcategory subcategory : subcategories) {
            String subName = subcategory.getSubcategoryName();
            
            if (subName.contains("Education")) {
                addToGroup(groupsMap, GROUP_EDUCATION, subcategory);
                logger.debug(LOG_MAPPED_SUBCATEGORY, subName, GROUP_EDUCATION);
            } else if (subName.contains("Foreign Work")) {
                addToGroup(groupsMap, GROUP_FOREIGN_WORK, subcategory);
                logger.debug(LOG_MAPPED_SUBCATEGORY, subName, GROUP_FOREIGN_WORK);
            } else if (subName.contains("Trades Certificate")) {
                tradesCertificates.add(subcategory);
                logger.debug(LOG_SKIPPING_TRADES, subName);
            }
        }
        
        int totalScore = 0;
        
        // Apply group caps (max 50 points per functional group)
        for (Map.Entry<String, List<EvaluationSubcategory>> entry : groupsMap.entrySet()) {
            String groupName = entry.getKey();
            List<EvaluationSubcategory> groupSubcategories = entry.getValue();
            
            // Calculate group total
            int groupScore = groupSubcategories.stream()
                    .mapToInt(EvaluationSubcategory::getUserScore)
                    .sum();
            
            // Check if group score exceeds cap
            if (groupScore > MAX_POINTS_PER_GROUP) {
                logger.info(LOG_GROUP_EXCEEDS_CAP, groupName, groupScore, MAX_POINTS_PER_GROUP);
                
                if (cappingDetails != null) {
                    cappingDetails.addShortNote(String.format(FORMAT_GROUP_CAPPED, 
                        groupName, groupScore, MAX_POINTS_PER_GROUP));
                    cappingDetails.recordGroupScores(groupName, groupScore, MAX_POINTS_PER_GROUP);
                }
                
                // Apply proportional reduction to subcategories
                applyProportionalReduction(groupSubcategories, MAX_POINTS_PER_GROUP, cappingDetails);
                
                totalScore += MAX_POINTS_PER_GROUP;
                logger.debug(LOG_GROUP_CONTRIBUTING, 
                           groupName, MAX_POINTS_PER_GROUP);
            } else {
                // No capping needed for this group
                totalScore += groupScore;
                logger.debug(LOG_GROUP_CONTRIBUTING, 
                           groupName, groupScore);
            }
        }
        
        // Add trade certificates score without capping at the group level
        int tradesScore = tradesCertificates.stream()
                .mapToInt(EvaluationSubcategory::getUserScore)
                .sum();
        totalScore += tradesScore;
        
        if (!tradesCertificates.isEmpty()) {
            logger.debug(LOG_TRADES_CONTRIBUTING, 
                       tradesScore);
        }
        
        // Apply overall category cap of 100 points
        if (totalScore > MAX_CATEGORY_POINTS) {
            if (cappingDetails != null) {
                cappingDetails.addShortNote(String.format(FORMAT_CATEGORY_CAPPING, 
                    totalScore, MAX_CATEGORY_POINTS));
                cappingDetails.recordTotalScores(totalScore, MAX_CATEGORY_POINTS);
            }
            
            logger.info(LOG_SCORE_EXCEEDS_CAP, 
                       totalScore, MAX_CATEGORY_POINTS);
            totalScore = MAX_CATEGORY_POINTS;
        }
        
        logger.info(LOG_FINAL_SCORE, totalScore);
        return totalScore;
    }
    
    /**
     * Applies proportional reduction to subcategories to ensure the total doesn't exceed the cap.
     * Updates and saves the subcategories with their new scores.
     * 
     * @param subcategories List of subcategories to adjust
     * @param capLimit Maximum allowed total score
     */
    private void applyProportionalReduction(List<EvaluationSubcategory> subcategories, int capLimit, CappingDetails cappingDetails) {
        if (subcategories == null || subcategories.isEmpty()) {
            return;
        }
        
        int totalOriginalScore = subcategories.stream()
                .mapToInt(EvaluationSubcategory::getUserScore)
                .sum();
        
        // Skip processing if total is already below the cap
        if (totalOriginalScore <= capLimit) {
            return;
        }
        
        // Calculate the proportional reduction factor
        double scaleFactor = (double) capLimit / totalOriginalScore;
        
        int lastIndex = subcategories.size() - 1;
        int runningSum = 0;
        
        for (int i = 0; i < subcategories.size(); i++) {
            EvaluationSubcategory subcategory = subcategories.get(i);
            int originalScore = subcategory.getUserScore();
            
            // Special handling for the last item to ensure the total is exactly the cap
            if (i == lastIndex) {
                int newScore = capLimit - runningSum;
                // Ensure we don't go negative if there are rounding issues
                newScore = Math.max(MIN_SCORE, newScore);
                
                // Only update and save if the score actually changes
                if (newScore != originalScore) {
                    subcategory.setUserScore(newScore);
                    subcategoryRepository.save(subcategory);
                    
                    if (cappingDetails != null) {
                        cappingDetails.addDetailedNote(String.format(FORMAT_LAST_ADJUSTMENT, 
                            subcategory.getSubcategoryName(), originalScore, newScore));
                        cappingDetails.recordSubcategoryAdjustment(subcategory.getSubcategoryName());
                    }
                    
                    logger.debug(LOG_ADJUSTED_LAST_SUBCATEGORY, 
                               subcategory.getSubcategoryName(), originalScore, newScore);
                }
            } else {
                // For all other items, apply proportional reduction
                int newScore = (int) Math.round(originalScore * scaleFactor);
                runningSum += newScore;
                
                // Only update and save if the score actually changes
                if (newScore != originalScore) {
                    subcategory.setUserScore(newScore);
                    subcategoryRepository.save(subcategory);
                    
                    if (cappingDetails != null) {
                        cappingDetails.addDetailedNote(String.format(FORMAT_ADJUSTMENT_RESULT, 
                            subcategory.getSubcategoryName(), originalScore, newScore));
                        cappingDetails.recordSubcategoryAdjustment(subcategory.getSubcategoryName());
                    }
                    
                    logger.debug(LOG_PROPORTIONAL_ADJUSTMENT, 
                               subcategory.getSubcategoryName(), originalScore, newScore);
                }
            }
        }
    }

    /**
     * Helper method to add a subcategory to a group map
     * 
     * @param groupsMap Map of group names to subcategories
     * @param groupName Name of the group to add to
     * @param subcategory Subcategory to add to the group
     */
    private void addToGroup(Map<String, List<EvaluationSubcategory>> groupsMap, String groupName, EvaluationSubcategory subcategory) {
        if (!groupsMap.containsKey(groupName)) {
            groupsMap.put(groupName, new ArrayList<>());
        }
        groupsMap.get(groupName).add(subcategory);
    }
} 