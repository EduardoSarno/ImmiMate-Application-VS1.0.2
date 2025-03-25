package co.immimate.profile.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import  co.immimate.profile.model.JobsNoc;


/**
 * Repository for accessing NOC (National Occupational Classification) codes and 
 * their TEER categories from the database.
 */
@Repository
public interface JobsNocRepository extends JpaRepository<JobsNoc, Integer> {
    
    /**
     * Find a JobsNoc entry by its NOC code.
     * 
     * @param nocCode The NOC code to search for
     * @return An Optional containing the JobsNoc entry if found, empty otherwise
     */
    Optional<JobsNoc> findByNocCode(Integer nocCode);
    
    /**
     * Find a JobsNoc entry by its NOC code represented as a String.
     * This method safely converts the string to an integer before querying.
     * Useful for handling legacy or mixed data types.
     * 
     * @param nocCodeStr The NOC code as a string to search for
     * @return An Optional containing the JobsNoc entry if found, empty otherwise
     */
    @Query("SELECT j FROM JobsNoc j WHERE j.nocCode = CAST(:nocCodeStr AS integer)")
    Optional<JobsNoc> findByNocCodeString(@Param("nocCodeStr") String nocCodeStr);
} 