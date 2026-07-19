package com.dumy.module.session.service;

import com.dumy.module.session.entity.ESessionStatus;
import com.dumy.module.session.entity.Session;
import com.dumy.exception.BusinessException;
import com.dumy.exception.ErrorCode;
import com.dumy.module.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;

    @Value("${app.session.ttl-hours}")
    private long ttlHours;

    @Override
    @Transactional
    public Session create(String userId, String tenantId) {
        Session session = Session.builder()
                .userId(userId)
                .tenantId(tenantId)
                .expiresAt(Instant.now().plus(ttlHours, ChronoUnit.HOURS))
                .build();
        return sessionRepository.save(session);
    }

    @Override
    public Session validate(String token) {
        Session session = sessionRepository.findById(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERROR_401_3002));

        if (session.getStatus() == ESessionStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.ERROR_401_3004);
        }
        if (session.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.ERROR_401_3003);
        }
        return session;
    }

    @Override
    @Transactional
    public void revoke(String token) {
        Session session = sessionRepository.findById(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERROR_401_3002));
        session.setStatus(ESessionStatus.INACTIVE);
        sessionRepository.save(session);
    }
}
