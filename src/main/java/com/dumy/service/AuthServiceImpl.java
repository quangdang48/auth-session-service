package com.dumy.service;

import com.dumy.dto.LoginRequest;
import com.dumy.dto.LoginResponse;
import com.dumy.dto.RegisterB2BRequest;
import com.dumy.dto.RegisterB2CRequest;
import com.dumy.dto.UserResponse;
import com.dumy.entity.ERole;
import com.dumy.entity.ETenantUserStatus;
import com.dumy.entity.EUserType;
import com.dumy.entity.Session;
import com.dumy.entity.Tenant;
import com.dumy.entity.TenantUser;
import com.dumy.entity.User;
import com.dumy.exception.BusinessException;
import com.dumy.exception.ErrorCode;
import com.dumy.mapper.UserMapper;
import com.dumy.repository.TenantRepository;
import com.dumy.repository.TenantUserRepository;
import com.dumy.repository.UserRepository;
import com.dumy.session.SessionContext;
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
    private final SessionService sessionService;

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
                .domain(request.getDomain())
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

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        if (request.getDomain() != null && !request.getDomain().isBlank()) {
            return loginB2B(request);
        }
        return loginB2C(request);
    }

    private LoginResponse loginB2C(LoginRequest request) {
        User user = requireTypedUser(request.getUsername(), EUserType.B2C);
        requireCorrectPassword(user, request.getPassword());

        Session session = sessionService.create(user.getId(), null);
        return LoginResponse.builder().sessionToken(session.getId()).build();
    }

    private LoginResponse loginB2B(LoginRequest request) {
        User user = requireTypedUser(request.getUsername(), EUserType.B2B);
        requireCorrectPassword(user, request.getPassword());

        Tenant tenant = tenantRepository.findByDomain(request.getDomain())
                .orElseThrow(() -> new BusinessException(ErrorCode.ERROR_404_3005));

        tenantUserRepository.findByTenant_IdAndUser_Id(tenant.getId(), user.getId())
                .filter(tu -> tu.getStatus() == ETenantUserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERROR_403_3006));

        Session session = sessionService.create(user.getId(), tenant.getId());
        return LoginResponse.builder().sessionToken(session.getId()).tenantId(tenant.getId()).build();
    }

    @Override
    public void logout() {
        sessionService.revoke(SessionContext.getSessionId());
    }

    private User requireTypedUser(String username, EUserType expectedType) {
        return userRepository.findByUsernameAndUserType(username, expectedType)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERROR_401_3001));
    }

    private void requireCorrectPassword(User user, String password) {
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.ERROR_401_3001);
        }
    }

    private void requireUsernameAvailable(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.ERROR_409_2002);
        }
    }
}
