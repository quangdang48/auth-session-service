package com.dumy.service;

import com.dumy.dto.RegisterB2BRequest;
import com.dumy.dto.RegisterB2CRequest;
import com.dumy.dto.UserResponse;
import com.dumy.entity.ERole;
import com.dumy.entity.EUserType;
import com.dumy.entity.Tenant;
import com.dumy.entity.TenantUser;
import com.dumy.entity.User;
import com.dumy.exception.BusinessException;
import com.dumy.exception.ErrorCode;
import com.dumy.mapper.UserMapper;
import com.dumy.repository.TenantRepository;
import com.dumy.repository.TenantUserRepository;
import com.dumy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String TENANT_OWNER_STATUS = "ACTIVE";

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TenantUserRepository tenantUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse registerB2C(RegisterB2CRequest request) {
        requireUsernameAvailable(request.getUsername());

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .userType(EUserType.B2C)
                .build();
        userRepository.save(user);

        return userMapper.toResponse(user, null);
    }

    @Override
    @Transactional
    public UserResponse registerB2B(RegisterB2BRequest request) {
        requireUsernameAvailable(request.getUsername());

        Tenant tenant = Tenant.builder()
                .name(request.getTenantName())
                .build();
        tenantRepository.save(tenant);

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .userType(EUserType.B2B)
                .build();
        userRepository.save(user);

        TenantUser tenantUser = TenantUser.builder()
                .tenant(tenant)
                .user(user)
                .role(ERole.OWNER)
                .build();
        tenantUserRepository.save(tenantUser);

        return userMapper.toResponse(user, tenant);
    }

    private void requireUsernameAvailable(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.ERROR_409_2002);
        }
    }
}
