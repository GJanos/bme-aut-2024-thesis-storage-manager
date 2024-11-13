package com.bme.vik.aut.thesis.depot.security.user;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @Test
    void ShouldReturnCorrectPermissionsForEveryRole() {
        Role role = Role.USER;
        assertEquals(Set.of(Permission.USER_CREATE, Permission.USER_READ, Permission.USER_UPDATE, Permission.USER_DELETE), role.getPermissions());

        role = Role.SUPPLIER;
        assertEquals(Set.of(Permission.USER_CREATE, Permission.USER_READ, Permission.USER_UPDATE, Permission.USER_DELETE,
                Permission.SUPPLIER_CREATE, Permission.SUPPLIER_READ, Permission.SUPPLIER_UPDATE, Permission.SUPPLIER_DELETE), role.getPermissions());

        role = Role.ADMIN;
        assertEquals(Set.of(Permission.USER_CREATE, Permission.USER_READ, Permission.USER_UPDATE, Permission.USER_DELETE,
                Permission.ADMIN_CREATE, Permission.ADMIN_READ, Permission.ADMIN_UPDATE, Permission.ADMIN_DELETE), role.getPermissions());
    }

    @Test
    void shouldReturnCorrectAuthoritiesForUserRole() {
        Role role = Role.USER;
        List<SimpleGrantedAuthority> authorities = role.getAuthorities();

        assertEquals(5, authorities.size());  // 4 permissions + 1 ROLE_USER
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("user:create")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("user:read")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("user:update")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("user:delete")));
    }

    @Test
    void shouldReturnCorrectAuthoritiesForSupplierRole() {
        Role role = Role.SUPPLIER;
        List<SimpleGrantedAuthority> authorities = role.getAuthorities();

        assertEquals(9, authorities.size());  // 8 permissions + 1 ROLE_SUPPLIER
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_SUPPLIER")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("user:create")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("user:read")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("user:update")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("user:delete")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("supplier:create")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("supplier:read")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("supplier:update")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("supplier:delete")));
    }

    @Test
    void shouldReturnCorrectAuthoritiesForAdminRole() {
        Role role = Role.ADMIN;
        List<SimpleGrantedAuthority> authorities = role.getAuthorities();

        assertEquals(9, authorities.size());  // 12 permissions + 1 ROLE_ADMIN
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertTrue(authorities.contains(new SimpleGrantedAuthority("user:create")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("user:read")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("user:update")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("user:delete")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("admin:create")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("admin:read")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("admin:update")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("admin:delete")));
    }

    @Test
    void ShouldReturnAllRolesWhenValuesCalled() {
        Role[] roles = Role.values();
        assertEquals(3, roles.length);
        assertTrue(Arrays.asList(roles).contains(Role.USER));
        assertTrue(Arrays.asList(roles).contains(Role.SUPPLIER));
        assertTrue(Arrays.asList(roles).contains(Role.ADMIN));
    }
}