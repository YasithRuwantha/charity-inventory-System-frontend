package com.charitymanagement.api.charitymanagementback.common.security;

/** SpEL fragments reused by {@code @PreAuthorize}, so role rules stay consistent across controllers. */
public final class Roles {

    public static final String ADMIN = "hasRole('ADMIN')";
    public static final String STAFF = "hasAnyRole('ADMIN','INVENTORY_STAFF')";
    public static final String ANY_AUTHENTICATED = "isAuthenticated()";

    private Roles() {
    }
}
