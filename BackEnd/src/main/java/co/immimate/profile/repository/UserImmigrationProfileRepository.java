package co.immimate.profile.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.immimate.profile.model.UserImmigrationProfile;
import co.immimate.user.model.User;

@Repository
public interface UserImmigrationProfileRepository extends JpaRepository<UserImmigrationProfile, UUID> {
    /**
     * Find all profiles associated with a specific user
     */
    List<UserImmigrationProfile> findByUser(User user);
    
    /**
     * Find all profiles by user email
     */
    List<UserImmigrationProfile> findByUserEmail(String email);
    
    /**
     * Find the most recent profile for a user
     */
    Optional<UserImmigrationProfile> findFirstByUserOrderByCreatedAtDesc(User user);
    
    /**
     * Find the most recent profile by user email
     */
    Optional<UserImmigrationProfile> findFirstByUserEmailOrderByCreatedAtDesc(String email);
    /**
     * Finds a user immigration profile by its application ID.
     * 
     * @param applicationId The UUID of the application to search for
     * @return An Optional containing the profile if found, or empty if not found
     */
    Optional<UserImmigrationProfile> findByApplicationId(UUID applicationId);
} 