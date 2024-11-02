package com.bme.vik.aut.thesis.depot.security.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Permission {

    ADMIN_CREATE("admin:create"),
    ADMIN_READ("admin:read"),
    ADMIN_UPDATE("admin:update"),
    ADMIN_DELETE("admin:delete"),

    SUPPLIER_CREATE("supplier:create"),
    SUPPLIER_READ("supplier:read"),
    SUPPLIER_UPDATE("supplier:update"),
    SUPPLIER_DELETE("supplier:delete"),

    USER_CREATE("user:create"),
    USER_READ("user:read"),
    USER_UPDATE("user:update"),
    USER_DELETE("user:delete");

    private final String permission;
}
