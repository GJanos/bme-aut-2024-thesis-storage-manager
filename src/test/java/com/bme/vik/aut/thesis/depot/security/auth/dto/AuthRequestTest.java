package com.bme.vik.aut.thesis.depot.security.auth.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthRequestTest {

    @Test
    void testBuilderAndGetters() {
        String userName = "testUser";
        String password = "testPassword";

        AuthRequest request = AuthRequest.builder()
                .userName(userName)
                .password(password)
                .build();

        assertEquals(userName, request.getUserName());
        assertEquals(password, request.getPassword());
    }

    @Test
    void testSetters() {
        AuthRequest request = new AuthRequest();
        request.setUserName("newUser");
        request.setPassword("newPassword");

        assertEquals("newUser", request.getUserName());
        assertEquals("newPassword", request.getPassword());
    }

    @Test
    void testNoArgsConstructor() {
        AuthRequest request = new AuthRequest();
        assertNull(request.getUserName());
        assertNull(request.getPassword());
    }

    @Test
    void testAllArgsConstructor() {
        AuthRequest request = new AuthRequest("testUser", "testPassword");
        assertEquals("testUser", request.getUserName());
        assertEquals("testPassword", request.getPassword());
    }

    @Test
    void testToString() {
        AuthRequest request = AuthRequest.builder()
                .userName("testUser")
                .password("testPassword")
                .build();
        String expectedString = "AuthRequest(userName=testUser, password=testPassword)";
        assertEquals(expectedString, request.toString());
    }

    @Test
    void testEqualsAndHashCode() {
        AuthRequest request1 = new AuthRequest("testUser", "testPassword");
        AuthRequest request2 = new AuthRequest("testUser", "testPassword");

        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }
}
