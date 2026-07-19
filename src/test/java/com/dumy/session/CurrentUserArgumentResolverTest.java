package com.dumy.session;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentUserArgumentResolverTest {

    private final CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver();

    @AfterEach
    void cleanUp() {
        SessionContext.clear();
    }

    @Test
    void resolvesPrincipalFromSessionContext() throws NoSuchMethodException {
        SessionContext.set("session-1", "user-1", "tenant-1");
        MethodParameter parameter = parameterOf("handler");

        CurrentUserPrincipal principal = (CurrentUserPrincipal) resolver.resolveArgument(parameter, null, null, null);

        assertThat(principal.userId()).isEqualTo("user-1");
        assertThat(principal.tenantId()).isEqualTo("tenant-1");
    }

    @Test
    void supportsOnlyAnnotatedCurrentUserPrincipalParameters() throws NoSuchMethodException {
        assertThat(resolver.supportsParameter(parameterOf("handler"))).isTrue();
        assertThat(resolver.supportsParameter(parameterOf("unannotatedHandler"))).isFalse();
    }

    private MethodParameter parameterOf(String methodName) throws NoSuchMethodException {
        return new MethodParameter(Fixture.class.getDeclaredMethod(methodName, CurrentUserPrincipal.class), 0);
    }

    private static class Fixture {
        void handler(@CurrentUser CurrentUserPrincipal principal) {
        }

        void unannotatedHandler(CurrentUserPrincipal principal) {
        }
    }
}
