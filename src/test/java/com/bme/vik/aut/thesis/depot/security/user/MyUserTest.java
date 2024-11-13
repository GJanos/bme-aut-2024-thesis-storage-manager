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
        //***** <-- given: Create user under test --> *****//
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
        //***** <-- when: Call onCreate method --> *****//
        user.onCreate();

        //***** <-- then: Verify createdAt and updatedAt are set and equal --> *****//
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());

        // Truncate both timestamps to milliseconds for comparison
        LocalDateTime createdAtTruncated = user.getCreatedAt().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime updatedAtTruncated = user.getUpdatedAt().truncatedTo(ChronoUnit.MILLIS);

        assertEquals(createdAtTruncated, updatedAtTruncated);
    }

    @Test
    void shouldSetUpdatedAtWhenOnUpdateCalled() {
        //***** <-- when: Set initial createdAt time --> *****//
        LocalDateTime initialCreatedAt = LocalDateTime.now().minusDays(1);
        user.setCreatedAt(initialCreatedAt);
        user.onUpdate();

        //***** <-- then: Verify updatedAt is set and after createdAt --> *****//
        assertNotNull(user.getUpdatedAt());
        assertTrue(user.getUpdatedAt().isAfter(user.getCreatedAt()));
    }

    @Test
    void shouldReturn5AuthoritiesWhenRoleIsUser() {
        //***** <-- given --> *****//
        user.setRole(Role.USER);

        //***** <-- when --> *****//
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        //***** <-- then: Verify authorities --> *****//
        // 4 User permissions + 1 Role
        assertEquals(5, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void shouldReturn9AuthoritiesWhenRoleIsSupplier() {
        //***** <-- given --> *****//
        user.setRole(Role.SUPPLIER);

        //***** <-- when --> *****//
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        //***** <-- then: Verify authorities --> *****//
        // 4 User permissions + 4 Supplier permissions + 1 Role
        assertEquals(9, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_SUPPLIER")));
    }

    @Test
    void shouldReturn13AuthoritiesWhenRoleIsAdmin() {
        //***** <-- given --> *****//
        user.setRole(Role.ADMIN);

        //***** <-- when --> *****//
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        //***** <-- then: Verify authorities --> *****//
        // 4 User permissions + 4 Supplier permissions + 4 Admin permissions + 1 Role
        assertEquals(9, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
