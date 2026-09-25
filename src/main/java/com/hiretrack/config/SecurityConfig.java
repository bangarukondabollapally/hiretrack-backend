package com.hiretrack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * SecurityConfig — TASK-001 baseline.
 *
 * Responsibilities in this task:
 *  - Disable CSRF (stateless JWT API — no session cookies).
 *  - Set session management to STATELESS.
 *  - Permit /api/health and /api/auth/** without a token.
 *  - Require authentication for all other endpoints (placeholder — expanded in TASK-008).
 *  - Wire CORS to allow http://localhost:5173 (Vite dev server) for local development.
 *  - Register a BCryptPasswordEncoder bean (used from TASK-006 onward).
 *
 * The JWT filter is added in TASK-008. Until then, all non-permitted endpoints
 * return 401 because no authentication mechanism is wired yet, which is correct.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — stateless JWT API; no session cookies used
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless session — JWT carries all auth state
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // CORS — delegate to the corsConfigurationSource bean below
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints — no JWT required
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                // Everything else requires authentication (JWT filter added in TASK-008)
                .anyRequest().authenticated()
            );

        return http.build();
    }

    /**
     * CORS configuration for local development.
     * Permits requests from http://localhost:5173 (Vite dev server).
     * Production origins are added when deployment configuration is finalized (TASK-038).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /**
     * BCryptPasswordEncoder bean — used by AuthService in TASK-006 for password hashing.
     * Registered here (in config/) so it is available as a dependency across the application.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
