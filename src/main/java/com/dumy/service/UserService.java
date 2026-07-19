package com.dumy.service;

import com.dumy.dto.UserProfileResponse;

public interface UserService {

    UserProfileResponse getCurrentUser(String userId);
}
