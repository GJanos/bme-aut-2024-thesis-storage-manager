package com.bme.vik.aut.thesis.depot.security.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class MyUserTest {

    MyUser user;

    @BeforeEach
    void setUp() {
        user = new MyUser();
    }

    @Test
    void shouldSetAndGetId() {
        Integer id = 1;
        user.setId(id);
        assertEquals(id, user.getId());
    }

    @Test
    void shouldSetAndGetUserName() {
        String username = "testUser";
        user.setUserName(username);
        assertEquals(username, user.getUsername());
    }

    @Test
    void shouldSetAndGetPassword() {
        String password = "testPassword";
        user.setPassword(password);
        assertEquals(password, user.getPassword());
    }

    @Test
    void shouldSetAndGetRole() {
        Role role = Role.ADMIN;
        user.setRole(role);
        assertEquals(role, user.getRole());
    }

    @Test
    void shouldSetAndGetCreatedAt() {
        LocalDateTime createdAt = LocalDateTime.now();
        user.setCreatedAt(createdAt);
        assertEquals(createdAt, user.getCreatedAt());
    }

    @Test
    void shouldSetAndGetUpdatedAt() {
        LocalDateTime updatedAt = LocalDateTime.now();
        user.setUpdatedAt(updatedAt);
        assertEquals(updatedAt, user.getUpdatedAt());
    }

    @Test
    void shouldReturnTrueForIsAccountNonExpired() {
        assertTrue(user.isAccountNonExpired());
    }

    @Test
    void shouldReturnTrueForIsAccountNonLocked() {
        assertTrue(user.isAccountNonLocked());
    }

    @Test
    void shouldReturnTrueForIsCredentialsNonExpired() {
        assertTrue(user.isCredentialsNonExpired());
    }

    @Test
    void shouldReturnTrueForIsEnabled() {
        assertTrue(user.isEnabled());
    }

    @Test
    void shouldSetCreatedAtWhenOnCreateCalled() {
        user.onCreate();
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());

        // Truncate both timestamps to milliseconds for comparison
        LocalDateTime createdAtTruncated = user.getCreatedAt().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime updatedAtTruncated = user.getUpdatedAt().truncatedTo(ChronoUnit.MILLIS);

        // On creation, both timestamps should be equal
        assertEquals(createdAtTruncated, updatedAtTruncated);
    }

    @Test
    void shouldSetUpdatedAtWhenOnUpdateCalled() {
        LocalDateTime initialCreatedAt = LocalDateTime.now().minusDays(1);
        user.setCreatedAt(initialCreatedAt);
        user.onUpdate();
        assertNotNull(user.getUpdatedAt());
        assertTrue(user.getUpdatedAt().isAfter(user.getCreatedAt())); // Updated time should be after created time
    }

    @Test
    void shouldReturn5AuthoritiesWhenRoleIsUser() {
        user.setRole(Role.USER);
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        // 4 User permissions + 1 Role
        assertEquals(5, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void shouldReturn9AuthoritiesWhenRoleIsSupplier() {
        user.setRole(Role.SUPPLIER);
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        // 4 User permissions + 4 Supplier permissions + 1 Role
        assertEquals(9, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_SUPPLIER")));
    }

    @Test
    void shouldReturn13AuthoritiesWhenRoleIsAdmin() {
        user.setRole(Role.ADMIN);
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
        // 4 User permissions + 4 Supplier permissions + 4 Admin permissions + 1 Role
        assertEquals(13, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

}
