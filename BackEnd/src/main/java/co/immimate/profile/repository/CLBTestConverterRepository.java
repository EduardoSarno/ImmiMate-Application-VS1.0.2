package co.immimate.profile.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.immimate.profile.model.CLBTestConverter;

/**
 * Repository for CLB test conversion data
 */
@Repository
public interface CLBTestConverterRepository extends JpaRepository<CLBTestConverter, Long> {
    
    /**
     * Find all conversion entries ordered by CLB level in descending order
     * @return List of CLB test converter entities
     */
    List<CLBTestConverter> findAllByOrderByClbLevelDesc();
    
    /**
     * Find all active conversion entries ordered by CLB level
     * @return List of active CLB test converter entities
     */
    List<CLBTestConverter> findByActiveIsTrueOrderByClbLevelDesc();
} 