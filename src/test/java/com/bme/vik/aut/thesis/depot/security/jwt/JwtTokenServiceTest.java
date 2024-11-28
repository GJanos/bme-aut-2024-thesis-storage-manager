package com.bme.vik.aut.thesis.depot.security.jwt;

import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import com.bme.vik.aut.thesis.depot.security.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(JwtTokenServiceTest.JwtTokenServiceTestConfig.class)
@TestPropertySource(properties = {
        "application.security.jwt.secret-key=rg26e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
        "application.security.jwt.expiration=3600000"  // 1 hour expiration
})
class JwtTokenServiceTest {

    @TestConfiguration
    static class JwtTokenServiceTestConfig {

        @Bean
        public JwtTokenService jwtTokenService() {
            return new JwtTokenService();
        }
    }

    @Autowired
    JwtTokenService jwtTokenService;

    final String USER_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJhdXRob3JpdGllcyI6WyJ1c2VyOmRlbGV0ZSIsInVzZXI6dXBkYXRlIiwidXNlcjpyZWFkIiwidXNlcjpjcmVhdGUiLCJST0xFX1VTRVIiXSwic3ViIjoiZGVwb3R1c2VyIiwiaWF0IjoxNzI5NzA4NzIxLCJleHAiOjE3MzgzNDg3MjF9.NQXKdN3IwODVRwU6MaK398AVYQ4_a6pWaa1dnp0CgP0";

    final String SUPPLIER_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJhdXRob3JpdGllcyI6WyJ1c2VyOmRlbGV0ZSIsInN1cHBsaWVyOnVwZGF0ZSIsInN1cHBsaWVyOmNyZWF0ZSIsInN1cHBsaWVyOmRlbGV0ZSIsInVzZXI6dXBkYXRlIiwidXNlcjpjcmVhdGUiLCJzdXBwbGllcjpyZWFkIiwidXNlcjpyZWFkIiwiUk9MRV9TVVBQTElFUiJdLCJzdWIiOiJzdXBwbGllciIsImlhdCI6MTczMTE0MzUwOCwiZXhwIjoxNzM5NzgzNTA4fQ.MwQ98s35PyAdHc4oLyrbugh6MaIuI9uzTH2ZXQc0g3U";

    final String ADMIN_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJhdXRob3JpdGllcyI6WyJ1c2VyOmRlbGV0ZSIsImFkbWluOnVwZGF0ZSIsImFkbWluOnJlYWQiLCJhZG1pbjpjcmVhdGUiLCJhZG1pbjpkZWxldGUiLCJ1c2VyOnVwZGF0ZSIsInVzZXI6Y3JlYXRlIiwidXNlcjpyZWFkIiwiUk9MRV9BRE1JTiJdLCJzdWIiOiJkZXBvdGFkbWluIiwiaWF0IjoxNzMxMTQzNTg3LCJleHAiOjE3Mzk3ODM1ODd9.FzjUYqAQx8hVl3cqAufIBC1LRKAjGzP4IiRlh3x0Tro";

    final String BAD_TOKEN = "badtoken.for.testing";

    final String EXPIRED_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJhdXRob3JpdGllcyI6WyJ1c2VyOnJlYWQiLCJ1c2VyOmNyZWF0ZSIsInVzZXI6dXBkYXRlIiwidXNlcjpkZWxldGUiLCJST0xFX1VTRVIiXSwic3ViIjoiZGVwb3R1c2VyIiwiaWF0IjoxNzI5ODAzNzQ5LCJleHAiOjE3Mjk4MDM3NDl9.Y6w0eI1GTYXqXF6QppqYQ3zQox77waQIM-z6e-Qzt2M";

    final String USER_NAME = "depotuser";
    final String USER_PASSWORD = "depotuser";
    final String SUPPLIER_NAME = "supplier";
    final String SUPPLIER_PASSWORD = "password";
    final String ADMIN_NAME = "depotadmin";
    final String ADMIN_PASSWORD = "depotadmin";

    private List<String> extractClaims(String token) {
        return jwtTokenService.extractClaim(token, claims -> ((List<?>) claims.get("authorities"))
                .stream()
                .map(Object::toString)
                .collect(Collectors.toList()));
    }

    @Test
    void shouldExtractCorrectUsernameFromUserToken() {
        String username = jwtTokenService.extractUsername(USER_TOKEN);
        assertEquals(USER_NAME, username);
    }

    @Test
    void shouldExtractCorrectUsernameFromSupplierToken() {
        String username = jwtTokenService.extractUsername(SUPPLIER_TOKEN);
        assertEquals(SUPPLIER_NAME, username);
    }

    @Test
    void shouldExtractCorrectUsernameFromAdminToken() {
        String username = jwtTokenService.extractUsername(ADMIN_TOKEN);
        assertEquals(ADMIN_NAME, username);
    }

    @Test
    void shouldThrowMalformedJwtExceptionForBadToken() {
        String userName = jwtTokenService.extractUsername(BAD_TOKEN);
        assertNull(userName, "Username should be null for bad token");
    }

    @Test
    void shouldExtractCorrectClaimFromUserToken() {
        List<String> authorities = extractClaims(USER_TOKEN);
        assertNotNull(authorities);
        assertTrue(authorities.contains("ROLE_USER"));
        assertTrue(authorities.contains("user:create"));
        assertTrue(authorities.contains("user:read"));
        assertTrue(authorities.contains("user:update"));
        assertTrue(authorities.contains("user:delete"));
    }

    @Test
    void shouldExtractCorrectClaimFromSupplierToken() {
        List<String> authorities = extractClaims(SUPPLIER_TOKEN);
        assertNotNull(authorities);
        assertTrue(authorities.contains("ROLE_SUPPLIER"));
        assertTrue(authorities.contains("user:create"));
        assertTrue(authorities.contains("user:read"));
        assertTrue(authorities.contains("user:update"));
        assertTrue(authorities.contains("user:delete"));
        assertTrue(authorities.contains("supplier:create"));
        assertTrue(authorities.contains("supplier:read"));
        assertTrue(authorities.contains("supplier:update"));
        assertTrue(authorities.contains("supplier:delete"));
    }

    @Test
    void shouldExtractCorrectClaimFromAdminToken() {
        List<String> authorities = extractClaims(ADMIN_TOKEN);
        assertNotNull(authorities);
        assertTrue(authorities.contains("ROLE_ADMIN"));
        assertTrue(authorities.contains("user:create"));
        assertTrue(authorities.contains("user:read"));
        assertTrue(authorities.contains("user:update"));
        assertTrue(authorities.contains("user:delete"));
        assertTrue(authorities.contains("admin:create"));
        assertTrue(authorities.contains("admin:read"));
        assertTrue(authorities.contains("admin:update"));
        assertTrue(authorities.contains("admin:delete"));
    }

    @Test
    void shouldNotExtractClaimFromBadToken() {
        List<String> claims = extractClaims(BAD_TOKEN);
        assertNull(claims, "Claims should be null for bad token");
    }

    @Test
    void shouldReturnTrueWhenTokenWithValidUserNameAndExpiryGiven() {
        //***** <-- given --> *****//
        MyUser user = MyUser.builder()
                .userName(USER_NAME)
                .password(USER_PASSWORD)
                .role(Role.USER)
                .build();

        //***** <-- when --> *****//
        boolean isValid = jwtTokenService.isTokenValid(USER_TOKEN, user);

        //***** <-- then --> *****//
        assertTrue(isValid, "Token should be valid for matching username and not expired");
    }

    @Test
    void shouldReturnFalseWhenTokenWithNotMatchingUserNameGiven() {
        //***** <-- given --> *****//
        MyUser user = MyUser.builder()
                .userName(ADMIN_NAME)
                .password(ADMIN_PASSWORD)
                .role(Role.ADMIN)
                .build();

        //***** <-- when --> *****//
        boolean isValid = jwtTokenService.isTokenValid(USER_TOKEN, user);

        //***** <-- then --> *****//
        assertFalse(isValid, "Token should be invalid for not matching username");
    }

    @Test
    void shouldReturnFalseWhenExpiredTokenGiven() {
        //***** <-- given --> *****//
        MyUser user = MyUser.builder()
                .userName(USER_NAME)
                .password(USER_PASSWORD)
                .role(Role.USER)
                .build();

        //***** <-- when --> *****//
        boolean isValid = jwtTokenService.isTokenValid(EXPIRED_TOKEN, user);

        //***** <-- then --> *****//
        assertFalse(isValid, "Expired token should be invalid");
    }

    @Test
    void shouldGenerateTokenWhenCorrectUserDetailsGiven() {
        //***** <-- given --> *****//
        MyUser user = MyUser.builder()
                .userName(USER_NAME)
                .password(USER_PASSWORD)
                .role(Role.USER)
                .build();

        //***** <-- when --> *****//
        String token = jwtTokenService.generateToken(user);

        //***** <-- then --> *****//
        assertNotNull(token);

        assertTrue(jwtTokenService.isTokenValid(token, user));

        String username = jwtTokenService.extractUsername(token);
        assertEquals(USER_NAME, username);

        List<String> authorities = extractClaims(token);
        assertNotNull(authorities);
        assertTrue(authorities.contains("ROLE_USER"));
        assertTrue(authorities.contains("user:create"));

        // token expiration
        Date expirationDate = jwtTokenService.extractExpiration(token);
        assertNotNull(expirationDate, "Token should have a valid expiration date");
        assertTrue(expirationDate.after(new Date()), "Token expiration should be in the future");

        // token structure
        String[] tokenParts = token.split("\\.");
        assertEquals(3, tokenParts.length, "JWT token should have 3 parts (header, payload, signature)");
    }

    @Test
    void shouldThrowExceptionWhenUserIsNull() {
        //***** <-- when and then --> *****//
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenService.generateToken(null);
        });
        assertEquals("User details cannot be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenUsernameIsNull() {
        //***** <-- given: null username --> *****//
        MyUser userWithNullUsername = MyUser.builder()
                .userName(null)
                .password(USER_PASSWORD)
                .role(Role.USER)
                .build();

        //***** <-- when and then --> *****//
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenService.generateToken(userWithNullUsername);
        });
        assertEquals("Username cannot be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenPasswordIsNull() {
        //***** <-- given: null password --> *****//
        MyUser userWithNullPassword = MyUser.builder()
                .userName(USER_NAME)
                .password(null)
                .role(Role.USER)
                .build();

        //***** <-- when and then --> *****//
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenService.generateToken(userWithNullPassword);
        });
        assertEquals("Password cannot be null", exception.getMessage());
    }
}