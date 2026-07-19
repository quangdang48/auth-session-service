package com.dumy.module.session.service;

import com.dumy.module.session.entity.ESessionStatus;
import com.dumy.module.session.entity.Session;
import com.dumy.exception.BusinessException;
import com.dumy.exception.ErrorCode;
import com.dumy.module.session.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    private SessionServiceImpl sessionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sessionService = new SessionServiceImpl(sessionRepository);
        ReflectionTestUtils.setField(sessionService, "ttlHours", 24L);
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createComputesExpiryFromTtl() {
        Session session = sessionService.create("user-1", null);

        assertThat(session.getUserId()).isEqualTo("user-1");
        assertThat(session.getTenantId()).isNull();
        assertThat(session.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void unknownTokenIsRejected() {
        when(sessionRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.validate("missing"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERROR_401_3002);
    }

    @Test
    void revokedTokenIsRejected() {
        Session session = Session.builder()
                .id("session-1")
                .userId("user-1")
                .status(ESessionStatus.INACTIVE)
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        when(sessionRepository.findById("session-1")).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> sessionService.validate("session-1"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERROR_401_3004);
    }

    @Test
    void expiredTokenIsRejected() {
        Session session = Session.builder()
                .id("session-1")
                .userId("user-1")
                .status(ESessionStatus.ACTIVE)
                .expiresAt(Instant.now().minusSeconds(60))
                .build();
        when(sessionRepository.findById("session-1")).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> sessionService.validate("session-1"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERROR_401_3003);
    }

    @Test
    void validTokenResolvesToSession() {
        Session session = Session.builder()
                .id("session-1")
                .userId("user-1")
                .tenantId("tenant-1")
                .status(ESessionStatus.ACTIVE)
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        when(sessionRepository.findById("session-1")).thenReturn(Optional.of(session));

        Session resolved = sessionService.validate("session-1");

        assertThat(resolved.getUserId()).isEqualTo("user-1");
        assertThat(resolved.getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    void revokeSetsStatusInactive() {
        Session session = Session.builder()
                .id("session-1")
                .userId("user-1")
                .status(ESessionStatus.ACTIVE)
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        when(sessionRepository.findById("session-1")).thenReturn(Optional.of(session));

        sessionService.revoke("session-1");

        assertThat(session.getStatus()).isEqualTo(ESessionStatus.INACTIVE);
    }
}
