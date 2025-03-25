package co.immimate.auth.security;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import co.immimate.user.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

/**
 * Utility class for handling JWT tokens
 */
@Component
public class JwtUtil {
    
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    
    // JWT claim keys
    private static final String CLAIM_ID = "id";
    private static final String CLAIM_ROLE = "role";

    // Constants for cookie settings
    private static final boolean HTTP_ONLY = true;
    private static final String SAME_SITE_LAX = "Lax";
    private static final int COOKIE_EXPIRED = 0;
    private static final int MILLISECONDS_TO_SECONDS = 1000;
    private static final String EMPTY_COOKIE_VALUE = "";
    
    // Error message constants
    private static final String ERR_INVALID_SIGNATURE = "Invalid JWT signature: {}";
    private static final String ERR_INVALID_TOKEN = "Invalid JWT token: {}";
    private static final String ERR_TOKEN_EXPIRED = "JWT token is expired: {}";
    private static final String ERR_TOKEN_UNSUPPORTED = "JWT token is unsupported: {}";
    private static final String ERR_CLAIMS_EMPTY = "JWT claims string is empty: {}";
    private static final String ERR_TOKEN_EXPIRATION = "Error checking token expiration: {}";
    private static final String ERR_VALIDATING_TOKEN = "Error validating token: {}";
    
    // Debug message constants
    private static final String DEBUG_TOKEN_VALID = "Token is valid for user: {}";
    private static final String DEBUG_TOKEN_INVALID = "Token validation failed for user: {}";
    private static final String DEBUG_USERNAME_NULL = "Username is null in token";
    private static final String DEBUG_USERNAME_MISMATCH = "Username mismatch: token={}, userDetails={}";
    private static final String DEBUG_TOKEN_EXPIRED = "Token is expired";

    // Secret key should be stored in application.properties
    @Value("${jwt.secret:immimate_jwt_secret_key}")
    private String SECRET_KEY;

    // Token validity in milliseconds (24 hours)
    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;
    
    // Cookie name for JWT token
    @Value("${jwt.cookie.name:jwt}")
    private String jwtCookieName;
    
    // Cookie secure flag - should be true in production
    @Value("${jwt.cookie.secure:false}")
    private boolean secureCookie;
    
    // Cookie path - default to root path
    @Value("${jwt.cookie.path:/}")
    private String cookiePath;

    /**
     * Extracts the username (email) from the JWT token
     * 
     * @param token JWT token
     * @return Username (email)
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from the JWT token
     * 
     * @param token JWT token
     * @return Expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts a specific claim from the JWT token
     * 
     * @param token JWT token
     * @param claimsResolver Function to extract the claim
     * @return Claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts all claims from the JWT token
     * 
     * @param token JWT token
     * @return All claims
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
        } catch (SignatureException e) {
            log.error(ERR_INVALID_SIGNATURE, e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.error(ERR_INVALID_TOKEN, e.getMessage());
            throw e;
        } catch (ExpiredJwtException e) {
            log.error(ERR_TOKEN_EXPIRED, e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.error(ERR_TOKEN_UNSUPPORTED, e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error(ERR_CLAIMS_EMPTY, e.getMessage());
            throw e;
        }
    }

    /**
     * Checks if the JWT token is expired
     * 
     * @param token JWT token
     * @return True if expired, false otherwise
     */
    private Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            log.error(ERR_TOKEN_EXPIRATION, e.getMessage());
            return true;
        }
    }

    /**
     * Generates a JWT token for the given user
     * 
     * @param user User for whom to generate the token
     * @return JWT token
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_ID, user.getId().toString());
        claims.put(CLAIM_ROLE, user.getRole());
        
        return createToken(claims, user.getEmail());
    }

    /**
     * Creates a JWT token with the given claims and subject (email)
     * 
     * @param claims Claims to include in the token
     * @param subject Subject (email) of the token
     * @return JWT token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    /**
     * Validates a JWT token against a user
     * 
     * @param token JWT token
     * @param userDetails User details
     * @return True if valid, false otherwise
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractEmail(token);
            boolean isValid = (username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token));
            if (isValid) {
                log.debug(DEBUG_TOKEN_VALID, username);
            } else {
                log.debug(DEBUG_TOKEN_INVALID, username);
                if (username == null) {
                    log.debug(DEBUG_USERNAME_NULL);
                } else if (!username.equals(userDetails.getUsername())) {
                    log.debug(DEBUG_USERNAME_MISMATCH, username, userDetails.getUsername());
                }
                if (isTokenExpired(token)) {
                    log.debug(DEBUG_TOKEN_EXPIRED);
                }
            }
            return isValid;
        } catch (Exception e) {
            log.error(ERR_VALIDATING_TOKEN, e.getMessage());
            return false;
        }
    }
    
    /**
     * Creates a JWT cookie with the token for the user
     * 
     * @param user User for whom to generate the token
     * @return Cookie with JWT token
     */
    public Cookie createJwtCookie(User user) {
        String token = generateToken(user);
        Cookie cookie = new Cookie(jwtCookieName, token);
        cookie.setHttpOnly(HTTP_ONLY);
        cookie.setSecure(secureCookie);
        cookie.setPath(cookiePath);
        cookie.setMaxAge((int)(jwtExpiration / MILLISECONDS_TO_SECONDS)); // Convert milliseconds to seconds
        return cookie;
    }
    
    /**
     * Adds a JWT cookie to the HTTP response
     * 
     * @param response HTTP response
     * @param user User for whom to generate the token
     */
    public void addJwtCookieToResponse(HttpServletResponse response, User user) {
        Cookie cookie = createJwtCookie(user);
        response.addCookie(cookie);
    }
    
    /**
     * Creates a JWT cookie using ResponseCookie builder (for additional options)
     * 
     * @param user User for whom to generate the token
     * @return ResponseCookie with JWT token
     */
    public ResponseCookie createJwtResponseCookie(User user) {
        String token = generateToken(user);
        return ResponseCookie.from(jwtCookieName, token)
                .httpOnly(HTTP_ONLY)
                .secure(secureCookie)
                .path(cookiePath)
                .maxAge(jwtExpiration / MILLISECONDS_TO_SECONDS) // Convert milliseconds to seconds
                .sameSite(SAME_SITE_LAX) // Helps prevent CSRF
                .build();
    }
    
    /**
     * Creates a cookie that clears the JWT token
     * 
     * @return Cookie that clears the JWT token
     */
    public Cookie createClearJwtCookie() {
        Cookie cookie = new Cookie(jwtCookieName, null);
        cookie.setHttpOnly(HTTP_ONLY);
        cookie.setSecure(secureCookie);
        cookie.setPath(cookiePath);
        cookie.setMaxAge(COOKIE_EXPIRED); // Immediately expires the cookie
        return cookie;
    }
    
    /**
     * Creates a ResponseCookie that clears the JWT token
     * 
     * @return ResponseCookie that clears the JWT token
     */
    public ResponseCookie createClearJwtResponseCookie() {
        return ResponseCookie.from(jwtCookieName, EMPTY_COOKIE_VALUE)
                .httpOnly(HTTP_ONLY)
                .secure(secureCookie)
                .path(cookiePath)
                .maxAge(COOKIE_EXPIRED) // Immediately expires the cookie
                .build();
    }
    
    /**
     * Adds a clear JWT cookie to the HTTP response
     * 
     * @param response HTTP response
     */
    public void addClearJwtCookieToResponse(HttpServletResponse response) {
        Cookie cookie = createClearJwtCookie();
        response.addCookie(cookie);
    }
} 