package co.immimate.auth.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import co.immimate.user.service.UserDetailsServiceImpl;

/**
 * Configuration class for Spring Security
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtAuthFilter jwtAuthFilter;
    
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Configure authentication manager builder
     */
    @Override
    public void configure(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
        authenticationManagerBuilder
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder);
    }

    /**
     * Bean for authentication manager
     */
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
    
    /**
     * Configure HTTP security
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .cors().and().csrf().disable()
            .exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint).and()
            // Use STATEFUL session management for OAuth2
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED).and()
            .authorizeRequests()
                // Auth endpoints that don't require authentication
                .antMatchers("/auth/login", "/auth/register").permitAll()
                // Swagger/API docs
                .antMatchers("/api/docs/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Static resources - HTML, CSS, JS, images
                .antMatchers("/", "/index.html").permitAll()
                .antMatchers("/Html/**", "/css/**", "/JavaScript/**", "/Data/**", "/images/**").permitAll()
                // Allow access to static files in Front-End directory
                .antMatchers("/Front-End/**").permitAll()
                // OAuth2 endpoints - ensure all OAuth2 related paths are permitted
                .antMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                // Allow all OPTIONS requests (for CORS preflight)
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // All other requests need authentication
                .anyRequest().authenticated()
                .and()
            .oauth2Login()
                .loginPage("http://localhost:3000/login")
                // Use our custom success handler
                .successHandler(oauth2AuthenticationSuccessHandler())
                .permitAll();

        // Add JWT filter before UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    }

    /**
     * Bean for CORS configuration
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
        configuration.setExposedHeaders(Arrays.asList("x-auth-token"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Custom success handler for OAuth2 login
     */
    @Bean
    public AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler() {
        // Use a simpler path structure to avoid mapping issues
        return new SimpleUrlAuthenticationSuccessHandler("/oauth2/login/success");
    }
} 