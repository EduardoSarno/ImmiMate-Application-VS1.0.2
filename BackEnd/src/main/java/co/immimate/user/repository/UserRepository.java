package co.immimate.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import co.immimate.user.model.User;

/**
 * Repository for User entity operations
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    /**
     * Find a user by email
     *
     * @param email User email
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Find a user by Google ID
     *
     * @param googleId Google ID from OAuth
     * @return Optional containing the user if found
     */
    Optional<User> findByGoogleId(String googleId);
    
    /**
     * Check if a user exists with the given email
     *
     * @param email User email
     * @return True if user exists, false otherwise
     */
    boolean existsByEmail(String email);
    
    /**
     * Delete a user by email
     * 
     * @param email User email
     */
    @Modifying
    @Transactional
    void deleteByEmail(String email);
} 