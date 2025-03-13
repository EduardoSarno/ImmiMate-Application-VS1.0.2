package co.immimate.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
     * Check if a user exists with the given email
     *
     * @param email User email
     * @return True if user exists, false otherwise
     */
    boolean existsByEmail(String email);
} 