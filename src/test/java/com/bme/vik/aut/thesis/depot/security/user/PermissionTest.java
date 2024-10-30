package com.bme.vik.aut.thesis.depot.security.user;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class PermissionTest {

    @Test
    void shouldReturnCorrectPermissionForUserCreate() {
        Permission permission = Permission.USER_CREATE;
        assertEquals("user:create", permission.getPermission());
    }

    @Test
    void shouldReturnCorrectPermissionForUserRead() {
        Permission permission = Permission.USER_READ;
        assertEquals("user:read", permission.getPermission());
    }

    @Test
    void shouldReturnCorrectPermissionForUserUpdate() {
        Permission permission = Permission.USER_UPDATE;
        assertEquals("user:update", permission.getPermission());
    }

    @Test
    void shouldReturnCorrectPermissionForUserDelete() {
        Permission permission = Permission.USER_DELETE;
        assertEquals("user:delete", permission.getPermission());
    }

    @Test
    void shouldReturnCorrectPermissionForSupplierCreate() {
        Permission permission = Permission.SUPPLIER_CREATE;
        assertEquals("supplier:create", permission.getPermission());
    }

    @Test
    void shouldReturnCorrectPermissionForSupplierRead() {
        Permission permission = Permission.SUPPLIER_READ;
        assertEquals("supplier:read", permission.getPermission());
    }

    @Test
    void shouldReturnCorrectPermissionForSupplierUpdate() {
        Permission permission = Permission.SUPPLIER_UPDATE;
        assertEquals("supplier:update", permission.getPermission());
    }

    @Test
    void shouldReturnCorrectPermissionForSupplierDelete() {
        Permission permission = Permission.SUPPLIER_DELETE;
        assertEquals("supplier:delete", permission.getPermission());
    }

    @Test
    void shouldReturnCorrectPermissionForAdminCreate() {
        Permission permission = Permission.ADMIN_CREATE;
        assertEquals("admin:create", permission.getPermission());
    }

    @Test
    void shouldReturnCorrectPermissionForAdminRead() {
        Permission permission = Permission.ADMIN_READ;
        assertEquals("admin:read", permission.getPermission());
    }

    @Test
    void shouldReturnCorrectPermissionForAdminUpdate() {
        Permission permission = Permission.ADMIN_UPDATE;
        assertEquals("admin:update", permission.getPermission());
    }

    @Test
    void shouldReturnCorrectPermissionForAdminDelete() {
        Permission permission = Permission.ADMIN_DELETE;
        assertEquals("admin:delete", permission.getPermission());
    }

    @Test
    void shouldHaveAllPermissionsWhenValuesCalled() {
        Permission[] permissions = Permission.values();
        assertEquals(12, permissions.length);
        assertTrue(Arrays.asList(permissions).contains(Permission.ADMIN_CREATE));
        assertTrue(Arrays.asList(permissions).contains(Permission.ADMIN_READ));
        assertTrue(Arrays.asList(permissions).contains(Permission.ADMIN_UPDATE));
        assertTrue(Arrays.asList(permissions).contains(Permission.ADMIN_DELETE));
        assertTrue(Arrays.asList(permissions).contains(Permission.SUPPLIER_CREATE));
        assertTrue(Arrays.asList(permissions).contains(Permission.SUPPLIER_READ));
        assertTrue(Arrays.asList(permissions).contains(Permission.SUPPLIER_UPDATE));
        assertTrue(Arrays.asList(permissions).contains(Permission.SUPPLIER_DELETE));
        assertTrue(Arrays.asList(permissions).contains(Permission.USER_CREATE));
        assertTrue(Arrays.asList(permissions).contains(Permission.USER_READ));
        assertTrue(Arrays.asList(permissions).contains(Permission.USER_UPDATE));
        assertTrue(Arrays.asList(permissions).contains(Permission.USER_DELETE));
    }
}