package com.bme.vik.aut.thesis.depot.security.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Getter
@RequiredArgsConstructor
public enum Role {

    // base: user permissions
    // supplier: supplier + user permissions
    // admin: admin + user permissions
    USER(Set.of(
            Permission.USER_CREATE,
            Permission.USER_READ,
            Permission.USER_UPDATE,
            Permission.USER_DELETE)),

    SUPPLIER(combinePermissions(USER.getPermissions(),
            Permission.SUPPLIER_CREATE,
            Permission.SUPPLIER_READ,
            Permission.SUPPLIER_UPDATE,
            Permission.SUPPLIER_DELETE)),

    ADMIN(combinePermissions(USER.getPermissions(),
            Permission.ADMIN_CREATE,
            Permission.ADMIN_READ,
            Permission.ADMIN_UPDATE,
            Permission.ADMIN_DELETE));

    private final Set<Permission> permissions;

    // Helper method to combine permissions
    private static Set<Permission> combinePermissions(Set<Permission> basePermissions, Permission... additionalPermissions) {
        Set<Permission> allPermissions = new HashSet<>(basePermissions);
        allPermissions.addAll(Set.of(additionalPermissions));
        return allPermissions;
    }

    public List<SimpleGrantedAuthority> getAuthorities() {
        var authorities = permissions.stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getPermission()))
                .collect(Collectors.toList());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
        return authorities;
    }
}
