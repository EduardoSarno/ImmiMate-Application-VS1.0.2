package co.immimate.auth.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Class to handle authentication failures with 401 Unauthorized status
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String ERROR_RESPONSE_FORMAT = "{\"error\":\"Unauthorized\",\"message\":\"%s\"}";
    
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(CONTENT_TYPE_JSON);
        response.getWriter().write(String.format(ERROR_RESPONSE_FORMAT, authException.getMessage()));
    }
} 