package com.dumy.service;

import com.dumy.dto.RegisterB2BRequest;
import com.dumy.dto.RegisterB2CRequest;
import com.dumy.dto.UserResponse;

public interface AuthService {

    UserResponse registerB2C(RegisterB2CRequest request);

    UserResponse registerB2B(RegisterB2BRequest request);
}
