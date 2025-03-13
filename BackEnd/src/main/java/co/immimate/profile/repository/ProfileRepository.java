package co.immimate.profile.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.immimate.profile.model.UserImmigrationProfile;

/**
 * Repository interface for managing UserImmigrationProfile entities.
 * Extends JpaRepository to inherit standard CRUD operations.
 */
@Repository
public interface ProfileRepository extends JpaRepository<UserImmigrationProfile, UUID> {
    /**
     * Finds a user immigration profile by its application ID.
     * 
     * @param applicationId The UUID of the application to search for
     * @return An Optional containing the profile if found, or empty if not found
     */
    Optional<UserImmigrationProfile> findByApplicationId(UUID applicationId);
}


