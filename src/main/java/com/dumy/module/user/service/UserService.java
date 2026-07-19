package com.dumy.module.user.service;

import com.dumy.module.user.dto.UserProfileResponse;

public interface UserService {

    UserProfileResponse getCurrentUser(String userId);
}
