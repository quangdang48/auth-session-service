package com.dumy.session;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionContextTest {

    @AfterEach
    void cleanUp() {
        SessionContext.clear();
    }

    @Test
    void isAuthenticatedIsFalseWhenNothingIsSet() {
        assertThat(SessionContext.isAuthenticated()).isFalse();
    }

    @Test
    void setPopulatesIdentity() {
        SessionContext.set("session-1", "user-1", "tenant-1");

        assertThat(SessionContext.isAuthenticated()).isTrue();
        assertThat(SessionContext.getSessionId()).isEqualTo("session-1");
        assertThat(SessionContext.getUserId()).isEqualTo("user-1");
        assertThat(SessionContext.getTenantId()).contains("tenant-1");
    }

    @Test
    void clearRemovesIdentity() {
        SessionContext.set("session-1", "user-1", null);
        SessionContext.clear();

        assertThat(SessionContext.isAuthenticated()).isFalse();
        assertThatThrownBy(SessionContext::getUserId).isInstanceOf(IllegalStateException.class);
    }
}
