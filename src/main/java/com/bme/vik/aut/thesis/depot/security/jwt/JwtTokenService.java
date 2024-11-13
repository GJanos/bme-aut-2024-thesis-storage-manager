package com.bme.vik.aut.thesis.depot.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Setter
@Service
public class JwtTokenService {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenService.class);

    @Value("${application.security.jwt.secretkey}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        logger.info("Extracting username from token");
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        logger.info("Extracting expiration date from token");
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        logger.debug("Extracting claims from token");
        Optional<Claims> claims = extractAllClaims(token);
        return claims.map(claimsResolver).orElse(null);
    }

    public String generateToken(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("User details cannot be null");
        }
        if (userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }
        if (userDetails.getPassword() == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }

        logger.info("Generating JWT token for user: {}", userDetails.getUsername());

        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        long expiration = 100 * 24 * 60 * 60 * 1000L; // 100 days
        String token = buildToken(claims, userDetails.getUsername(), expiration);
        return token;
        //return buildToken(claims, userDetails.getUsername(), jwtExpiration);
    }

    private String buildToken(Map<String, Object> claims, String subject, long expiration) {
        logger.debug("Building JWT token with expiration: {}", expiration);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        logger.debug("Validating token for user: {}", userDetails.getUsername());
        Optional<Claims> claimsOpt = extractAllClaims(token);

        if (claimsOpt.isEmpty()) {
            logger.warn("Token is invalid or expired for user: {}", userDetails.getUsername());
            return false;
        }

        Claims claims = claimsOpt.get();
        final String username = claims.getSubject();
        boolean isValid = (username.equals(userDetails.getUsername()) && !isTokenExpired(claims));

        if (isValid) {
            logger.info("JWT token is valid for user: {}", userDetails.getUsername());
        } else {
            logger.warn("JWT token is invalid for user: {}", userDetails.getUsername());
        }

        return isValid;
    }

    private boolean isTokenExpired(Claims claims) {
        boolean expired = claims.getExpiration().before(new Date());
        if (expired) {
            logger.warn("JWT token expired at: {}", claims.getExpiration());
        }
        return expired;
    }

    private Optional<Claims> extractAllClaims(String token) {
        logger.debug("Extracting all claims from token");
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Optional.of(claims);
        } catch (ExpiredJwtException e) {
            logger.warn("Token has expired at: {}", e.getClaims().getExpiration());
            return Optional.empty();
        } catch (JwtException e) {
            logger.error("Error parsing claims from token", e);
            return Optional.empty();
        }
    }

    private Key getSignInKey() {
        logger.debug("Decoding secret key for token signing");
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
