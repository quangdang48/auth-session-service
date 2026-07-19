package com.dumy.module.session.web;

import java.util.Optional;

public final class SessionContext {

    private record Principal(String sessionId, String userId, String tenantId) {
    }

    private static final ThreadLocal<Principal> CONTEXT = new ThreadLocal<>();

    private SessionContext() {
    }

    public static void set(String sessionId, String userId, String tenantId) {
        CONTEXT.set(new Principal(sessionId, userId, tenantId));
    }

    public static String getSessionId() {
        return principal().sessionId();
    }

    public static String getUserId() {
        return principal().userId();
    }

    public static Optional<String> getTenantId() {
        return Optional.ofNullable(principal().tenantId());
    }

    public static boolean isAuthenticated() {
        return CONTEXT.get() != null;
    }

    public static void clear() {
        CONTEXT.remove();
    }

    private static Principal principal() {
        Principal principal = CONTEXT.get();
        if (principal == null) {
            throw new IllegalStateException("No session bound to the current request");
        }
        return principal;
    }
}
