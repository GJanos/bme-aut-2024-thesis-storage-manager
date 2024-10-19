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

    // permissions are defined in the order of user < supplier < admin
    // each role has the permissions of the weaker roles
    USER(Set.of(Permission.USER_CREATE,
            Permission.USER_READ,
            Permission.USER_UPDATE,
            Permission.USER_DELETE)),

    SUPPLIER(new HashSet<>() {{
        addAll(USER.getPermissions());
        addAll(Set.of(Permission.SUPPLIER_CREATE,
                Permission.SUPPLIER_READ,
                Permission.SUPPLIER_UPDATE,
                Permission.SUPPLIER_DELETE));
    }}),

    ADMIN(new HashSet<>() {{
        addAll(SUPPLIER.getPermissions());
        addAll(Set.of(Permission.ADMIN_CREATE,
                Permission.ADMIN_READ,
                Permission.ADMIN_UPDATE,
                Permission.ADMIN_DELETE));
    }});

    private final Set<Permission> permissions;

    public List<SimpleGrantedAuthority> getAuthorities() {
        var authorities = permissions.stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getPermission()))
                .collect(Collectors.toList());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
        return authorities;
    }
}
