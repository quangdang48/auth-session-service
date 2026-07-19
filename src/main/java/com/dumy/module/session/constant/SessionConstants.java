package com.dumy.module.session.constant;

import java.util.List;

public final class SessionConstants {

    public static final String SESSION_TOKEN_HEADER = "X-Session-Token";

    public static final String SESSION_SECURITY_SCHEME = "sessionAuth";

    public static final List<String> ALLOWLIST_PREFIXES = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/swagger-ui",
            "/v3/api-docs",
            "/h2-console"
    );

    private SessionConstants() {
    }
}
