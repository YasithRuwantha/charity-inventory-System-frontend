package com.charitymanagement.api.charitymanagementback.common.security;

/** Request attribute used to carry a token-rejection reason from the JWT filter to the entry point. */
public final class JwtErrorAttributes {

    public static final String JWT_ERROR_ATTRIBUTE = "charity.jwt.error";

    private JwtErrorAttributes() {
    }
}
