package com.dumy.module.session.service;

import com.dumy.module.session.entity.Session;

public interface SessionService {

    Session create(String userId, String tenantId);

    Session validate(String token);

    void revoke(String token);
}
