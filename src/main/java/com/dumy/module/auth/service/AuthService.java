package com.dumy.module.auth.service;

import com.dumy.module.auth.dto.LoginRequest;
import com.dumy.module.auth.dto.LoginResponse;
import com.dumy.module.auth.dto.RegisterB2BRequest;
import com.dumy.module.auth.dto.RegisterB2CRequest;
import com.dumy.module.auth.dto.UserResponse;

public interface AuthService {

    UserResponse registerB2C(RegisterB2CRequest request);

    UserResponse registerB2B(RegisterB2BRequest request);

    LoginResponse login(LoginRequest request);

    void logout();
}
