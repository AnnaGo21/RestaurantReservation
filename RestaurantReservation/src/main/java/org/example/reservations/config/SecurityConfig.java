package org.example.reservations.config;

import org.example.reservations.auth.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Swagger endpoints
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/restaurants").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/restaurants").permitAll()

                        // User management - roles defined in controller with @PreAuthorize
                        .requestMatchers("/api/users/**").authenticated()

                        // Restaurant management - OWNER only
                        .requestMatchers("/api/restaurants/**").hasRole("OWNER")

                        // Operations by role
                        .requestMatchers("/api/tables/**").hasAnyRole("OWNER", "MANAGER")
                        .requestMatchers("/api/guests/**").hasAnyRole("OWNER", "MANAGER", "STAFF")
                        .requestMatchers("/api/reservations/**").hasAnyRole("OWNER", "MANAGER", "STAFF")
                        .requestMatchers("/api/dashboard/**").hasAnyRole("OWNER", "MANAGER")
                        .requestMatchers("/api/analytics/**").hasAnyRole("OWNER", "MANAGER")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
