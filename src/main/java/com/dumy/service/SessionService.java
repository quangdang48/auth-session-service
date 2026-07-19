package com.dumy.service;

import com.dumy.entity.Session;

public interface SessionService {

    Session create(String userId, String tenantId);

    Session validate(String token);

    void revoke(String token);
}
