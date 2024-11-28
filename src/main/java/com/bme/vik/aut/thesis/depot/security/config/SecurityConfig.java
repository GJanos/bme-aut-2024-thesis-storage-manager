package com.bme.vik.aut.thesis.depot.security.config;

import com.bme.vik.aut.thesis.depot.security.jwt.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static com.bme.vik.aut.thesis.depot.security.user.Permission.USER_CREATE;
import static com.bme.vik.aut.thesis.depot.security.user.Permission.USER_READ;
import static com.bme.vik.aut.thesis.depot.security.user.Role.ADMIN;
import static com.bme.vik.aut.thesis.depot.security.user.Role.SUPPLIER;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] WHITE_LIST_URL = {
            "/auth/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/actuator/**",
    };

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable() // should be enabled if I had frontend
                .cors().disable()
                .authorizeHttpRequests(req ->
                        req.requestMatchers(WHITE_LIST_URL)
                                .permitAll()
                                .requestMatchers("/user/**").hasRole(ADMIN.name())
                                .requestMatchers("/admin/**").hasRole(ADMIN.name())
                                .requestMatchers("/report/**").hasRole(ADMIN.name())
                                .requestMatchers("/info/**").hasAnyAuthority(USER_READ.getPermission())
                                .requestMatchers(HttpMethod.POST, "/order").hasAnyAuthority(USER_CREATE.getPermission())
                                .requestMatchers("/order/**").hasRole(ADMIN.name())
                                .requestMatchers("/supplier/**").hasRole(SUPPLIER.name())
                                .anyRequest()
                                .authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}