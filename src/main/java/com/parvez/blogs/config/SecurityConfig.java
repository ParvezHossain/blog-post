package com.parvez.blogs.config;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;


@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 1. Specify your Frontend URL (DO NOT use "*" in production)
        configuration.setAllowedOrigins(List.of("https://your-blog-frontend.com", "http://localhost:3000"));

        // 2. Specify allowed HTTP Methods
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 3. Allow specific Headers (Required for JWT)
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control"));

        // 4. Allow Credentials (Required if you ever move JWT to Cookies)
        configuration.setAllowCredentials(true);

        // 5. Cache the pre-flight response for 1 hour to improve performance
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityWebFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable()) // Only disable if using JWT + Stateless
                .headers(headers -> headers
                        // Prevent Clickjacking (A01:2021)
                        // DENY: No one can frame your site.
                        // SAMEORIGIN: Only your own site can frame itself.
                        .frameOptions(frame -> frame.deny())

                        // Strict Transport Security (HSTS)
                        // Forces browsers to use HTTPS only
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)  // 1 year
                        )
                        // Content Security Policy (CSP) (A03:2021)
                        // Prevents XSS by defining which scripts can run
                        // XSS Protection: Use CSP (Content Security Policy) to block unauthorized scripts.
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; script-src 'self'; object-src 'none';")
                        )

                        // Permissions Policy
                        // Restricts browser features like camera/geolocation
                        .permissionsPolicyHeader(permissions ->
                                permissions.policy("camera=(), microphone=(), geolocation=()")
                        )

                )
                .authorizeHttpRequests(auth -> auth

                        /* ================= PUBLIC ================= */
                        .requestMatchers(
                                ApiPaths.LOGIN,
                                ApiPaths.REGISTER,
                                ApiPaths.FORGOT_PASSWORD,
                                ApiPaths.RESET_PASSWORD,
                                ApiPaths.HOME,
                                ApiPaths.REFRESH_TOKEN
                        ).permitAll()

                        // Allow GUESTS to view posts (GET requests only)
                        .requestMatchers(HttpMethod.GET, ApiPaths.POSTS + "/**").permitAll()

                        /* ================= PROTECTED ================= */

                        // Require ADMIN role for creating, updating, or deleting
                        .requestMatchers(HttpMethod.POST, ApiPaths.POSTS + "/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, ApiPaths.POSTS + "/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, ApiPaths.POSTS + "/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, ApiPaths.POSTS + "/**").hasRole("ADMIN")

                        /* ================= PROTECTED API ================= */
                        .requestMatchers(ApiPaths.API_BASE + "/**")
                        .authenticated()

                        /* ================= EVERYTHING ELSE ================= */
                        .anyRequest()
                        .permitAll()
                )
                .addFilterBefore(
                        jwtAuthFilter,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class
                );
        return http.build();
    }

    /* ===================== AUTH BEANS ===================== */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) {
        return authenticationConfiguration.getAuthenticationManager();
    }
}