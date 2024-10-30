package com.bme.vik.aut.thesis.depot.security.auth.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
// TODO might remove
class AuthResponseTest {

    @Test
    void testBuilderAndGetters() {
        String token = "sampleToken";

        AuthResponse response = AuthResponse.builder()
                .token(token)
                .build();

        assertEquals(token, response.getToken());
    }

    @Test
    void testSetters() {
        AuthResponse response = new AuthResponse();
        response.setToken("newToken");

        assertEquals("newToken", response.getToken());
    }

    @Test
    void testNoArgsConstructor() {
        AuthResponse response = new AuthResponse();
        assertNull(response.getToken());
    }

    @Test
    void testAllArgsConstructor() {
        AuthResponse response = new AuthResponse("sampleToken");
        assertEquals("sampleToken", response.getToken());
    }

    @Test
    void testToString() {
        AuthResponse response = AuthResponse.builder()
                .token("sampleToken")
                .build();
        String expectedString = "AuthResponse(token=sampleToken)";
        assertEquals(expectedString, response.toString());
    }

    @Test
    void testEqualsAndHashCode() {
        AuthResponse response1 = new AuthResponse("sampleToken");
        AuthResponse response2 = new AuthResponse("sampleToken");

        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
    }
}
