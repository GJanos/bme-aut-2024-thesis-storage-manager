package com.bme.vik.aut.thesis.depot.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtTokenService jwtTokenService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        logger.info("Incoming request METHOD: {} URI: {}", request.getMethod(), request.getRequestURI());

        // Extract the Authorization header
        final String authHeader = request.getHeader("Authorization");

        // Validate the Authorization header format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Authorization header is missing or does not start with 'Bearer'.");
            filterChain.doFilter(request, response);
            return;
        }

        // Extract the JWT token from the Authorization header
        String jwtToken = authHeader.substring(7);
        String userName = jwtTokenService.extractUsername(jwtToken);

        logger.info("JWT Token: {}", jwtToken);
        logger.info("Extracted User Name: {}", userName);

        if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userName);
            boolean isTokenValid = jwtTokenService.isTokenValid(jwtToken, userDetails);

            // If the token is valid, authenticate the user
            if (isTokenValid) {
                logger.info("JWT token is valid for user: {}", userName);
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                // Set additional details in the authentication object
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set the authentication in the SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                logger.warn("Invalid JWT token for user: {}", userName);
            }
        }else {
            logger.warn("Username is null or there is already an authentication in the SecurityContext.");
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }
}
