package com.bme.vik.aut.thesis.depot.security.auth.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RegisterRequestTest {

    @Test
    void testBuilderAndGetters() {
        String userName = "testUser";
        String password = "testPassword";

        RegisterRequest request = RegisterRequest.builder()
                .userName(userName)
                .password(password)
                .build();

        assertEquals(userName, request.getUserName());
        assertEquals(password, request.getPassword());
    }

    @Test
    void testSetters() {
        RegisterRequest request = new RegisterRequest();
        request.setUserName("newUser");
        request.setPassword("newPassword");

        assertEquals("newUser", request.getUserName());
        assertEquals("newPassword", request.getPassword());
    }

    @Test
    void testNoArgsConstructor() {
        RegisterRequest request = new RegisterRequest();
        assertNull(request.getUserName());
        assertNull(request.getPassword());
    }

    @Test
    void testAllArgsConstructor() {
        RegisterRequest request = new RegisterRequest("testUser", "testPassword");
        assertEquals("testUser", request.getUserName());
        assertEquals("testPassword", request.getPassword());
    }

    @Test
    void testToString() {
        RegisterRequest request = RegisterRequest.builder()
                .userName("testUser")
                .password("testPassword")
                .build();
        String expectedString = "RegisterRequest(userName=testUser, password=testPassword)";
        assertEquals(expectedString, request.toString());
    }

    @Test
    void testEqualsAndHashCode() {
        RegisterRequest request1 = new RegisterRequest("testUser", "testPassword");
        RegisterRequest request2 = new RegisterRequest("testUser", "testPassword");

        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }
}
