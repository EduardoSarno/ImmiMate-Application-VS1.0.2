package co.immimate.user.service;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.immimate.user.model.User;
import co.immimate.user.repository.UserRepository;

/**
 * Service for loading user-specific data for Spring Security
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    /**
     * Loads a user by username (email)
     *
     * @param email User email
     * @return UserDetails for Spring Security
     * @throws UsernameNotFoundException If user not found
     */
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("Loading user by email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole())
        );
        
        // Check if this might be an OAuth2 user with a null or empty password
        String password = user.getPassword();
        if (password == null || password.isEmpty()) {
            logger.info("User has no password (likely OAuth2 user). Using placeholder password.");
            password = "{noop}oauth2User"; // {noop} tells Spring not to try to match with an encoder
        } else {
            logger.debug("User has regular password");
        }

        logger.debug("Returning UserDetails for email: {}", email);
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                password,
                true,   // enabled
                true,   // accountNonExpired
                true,   // credentialsNonExpired
                true,   // accountNonLocked
                authorities
        );
    }
} 