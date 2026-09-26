package com.ahmed.hospital.config;

import com.ahmed.hospital.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                // CSRF is disabled intentionally: this API is fully stateless,
                // uses JWT Bearer tokens in Authorization header only (no cookies),
                // so CSRF attacks cannot be carried out against it.
                .csrf(AbstractHttpConfigurer::disable)

                // Enforce stateless session: no HTTP session is created or used.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // HTTP Security Headers
                .headers(headers -> headers
                        // Prevent MIME-type sniffing
                        .contentTypeOptions(contentTypeOptions -> {})
                        // Deny framing to prevent clickjacking
                        .frameOptions(frameOptions -> frameOptions.deny())
                        // HSTS for HTTPS environments (1 year, include subdomains)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                        // Referrer-Policy
                        .referrerPolicy(referrer -> referrer
                                .policy(
                                        ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN
                                )
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Public auth and docs endpoints
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                // WebSocket handshake endpoint must be allowed
                                "/ws/**"
                        ).permitAll()

                        // ─── Actuator: health sub-group endpoints ───────────────────────────
                        // /actuator/health        — public (Docker Compose / load-balancer probe)
                        // /actuator/health/**     — liveness + readiness groups, also public
                        //                           (Docker HEALTHCHECK, Kubernetes probes)
                        // /actuator/info          — public (safe static info: name, version)
                        // All other /actuator/**  — ADMIN only (see below)
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info"
                        ).permitAll()

                        .requestMatchers("/actuator/**")
                        .hasRole("ADMIN")


                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/api/doctor/**")
                        .hasRole("DOCTOR")

                        .requestMatchers("/api/receptionist/**")
                        .hasRole("RECEPTIONIST")

                        .requestMatchers("/api/patient/**")
                        .hasRole("PATIENT")

                        .anyRequest()
                        .authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}