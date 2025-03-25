package co.immimate.profile.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.immimate.profile.model.ProfileDraft;
import co.immimate.user.model.User;

/**
 * Repository for managing ProfileDraft entities.
 */
@Repository
public interface ProfileDraftRepository extends JpaRepository<ProfileDraft, UUID> {
    
    /**
     * Find all drafts for a specific user.
     * 
     * @param user The user
     * @return List of drafts
     */
    List<ProfileDraft> findByUser(User user);
    
    /**
     * Find all drafts for a specific user by email.
     * 
     * @param userEmail The user's email
     * @return List of drafts
     */
    List<ProfileDraft> findByUserEmail(String userEmail);
    
    /**
     * Find the most recent draft for a specific user.
     * 
     * @param user The user
     * @return The most recent draft or empty if none exists
     */
    Optional<ProfileDraft> findFirstByUserOrderByLastModifiedAtDesc(User user);
    
    /**
     * Find the most recent draft for a specific user by email.
     * 
     * @param userEmail The user's email
     * @return The most recent draft or empty if none exists
     */
    Optional<ProfileDraft> findFirstByUserEmailOrderByLastModifiedAtDesc(String userEmail);
} 