package com.asknehru.auth.config;

import com.asknehru.auth.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.disable())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/**",
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/refresh",
                                "/api/upload/**",
                                "/api/yoga/**",
                                "/api/roadmaps/import-syllabus/**",
                                "/error"
                        ).permitAll()
                        // Public read-only endpoints consumed by the Angular frontend without auth
                        .requestMatchers(HttpMethod.GET,
                                "/api/roadmaps",
                                "/api/roadmaps/**",
                                "/api/conversations",
                                "/api/conversations/**"
                        ).permitAll()
                        // Allow explain, MCQ, and chat endpoints for anonymous users
                        .requestMatchers(HttpMethod.POST,
                                "/api/conversations/explain",
                                "/api/conversations/*/mcq",
                                "/api/conversations/chat"
                        ).permitAll()
                        // Audio upload/delete — auth enforced inside controller via JWT super-admin check
                        .requestMatchers(HttpMethod.POST, "/api/conversations/*/audio").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/conversations/*/audio").authenticated()
                        // Allow admin toggle of user-assigned roadmap (React Admin has no auth)
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/roadmaps/*/user-assigned"
                        ).permitAll()
                        // Registration via user creation endpoint used by frontend
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
