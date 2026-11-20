package com.hexagonal.optin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * REST API secured with HTTP Basic, stateless: no session cookie and no CSRF token.
 * CSRF protects browser sessions; this API has no browser session, so clients such as
 * Postman or the scheduler can call the POST endpoints directly.
 * Health endpoints are public so Kubernetes and the GCP load balancer can probe the app.
 */
@Configuration
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Kubernetes probes and the load balancer health check call these without credentials.
                        // Only health is exposed over HTTP (management.endpoints.web.exposure.include=health).
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
