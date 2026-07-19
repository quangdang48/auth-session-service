package com.dumy.service;

import com.dumy.dto.LoginRequest;
import com.dumy.dto.LoginResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class AuthServiceImplLoginTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private TenantUserRepository tenantUserRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @Mock
    private SessionService sessionService;

    private AuthServiceImpl authService;

    private static final String USERNAME = "alice";
    private static final String PASSWORD = "password123";
    private static final String USER_ID = "user-1";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthServiceImpl(userRepository, tenantRepository, tenantUserRepository, passwordEncoder, userMapper, sessionService);
    }

    private User userOfType(EUserType userType) {
        return User.builder().id(USER_ID).username(USERNAME).password("hashed").userType(userType).build();
    }

    private void mockValidCredentials(EUserType userType) {
        when(userRepository.findByUsernameAndUserType(USERNAME, userType)).thenReturn(Optional.of(userOfType(userType)));
        when(passwordEncoder.matches(PASSWORD, "hashed")).thenReturn(true);
    }

    @Test
    void b2cUserLogsInDirectly() {
        mockValidCredentials(EUserType.B2C);
        when(sessionService.create(USER_ID, null)).thenReturn(
                Session.builder().id("session-1").userId(USER_ID).expiresAt(Instant.now().plusSeconds(60)).build());

        LoginResponse response = authService.login(loginRequest(null));

        assertThat(response.getSessionToken()).isEqualTo("session-1");
        assertThat(response.getTenantId()).isNull();
    }

    @Test
    void b2bUserOmittingDomainIsRejectedAsInvalidCredentials() {
        when(userRepository.findByUsernameAndUserType(USERNAME, EUserType.B2C)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest(null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERROR_401_3001);
    }

    @Test
    void b2cUserAttemptingDomainLoginIsRejectedAsInvalidCredentials() {
        when(userRepository.findByUsernameAndUserType(USERNAME, EUserType.B2B)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest("acme.io")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERROR_401_3001);
    }

    @Test
    void successfulDomainLoginIssuesTenantBoundSession() {
        mockValidCredentials(EUserType.B2B);
        Tenant tenant = Tenant.builder().id("tenant-1").name("Acme").domain("acme.io").build();
        when(tenantRepository.findByDomain("acme.io")).thenReturn(Optional.of(tenant));
        TenantUser membership = TenantUser.builder().tenant(tenant).status(ETenantUserStatus.ACTIVE).build();
        when(tenantUserRepository.findByTenant_IdAndUser_Id("tenant-1", USER_ID)).thenReturn(Optional.of(membership));
        when(sessionService.create(USER_ID, "tenant-1")).thenReturn(
                Session.builder().id("session-2").userId(USER_ID).tenantId("tenant-1").expiresAt(Instant.now().plusSeconds(60)).build());

        LoginResponse response = authService.login(loginRequest("acme.io"));

        assertThat(response.getSessionToken()).isEqualTo("session-2");
        assertThat(response.getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    void unknownDomainIsRejected() {
        mockValidCredentials(EUserType.B2B);
        when(tenantRepository.findByDomain("unknown.io")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest("unknown.io")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERROR_404_3005);
    }

    @Test
    void userNotAMemberOfDomainTenantIsRejected() {
        mockValidCredentials(EUserType.B2B);
        Tenant tenant = Tenant.builder().id("tenant-1").name("Acme").domain("acme.io").build();
        when(tenantRepository.findByDomain("acme.io")).thenReturn(Optional.of(tenant));
        when(tenantUserRepository.findByTenant_IdAndUser_Id("tenant-1", USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest("acme.io")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERROR_403_3006);
    }

    @Test
    void unknownUsernameIsRejectedWithInvalidCredentials() {
        when(userRepository.findByUsernameAndUserType(USERNAME, EUserType.B2C)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest(null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERROR_401_3001);
    }

    @Test
    void wrongPasswordIsRejectedWithSameErrorAsUnknownUsername() {
        when(userRepository.findByUsernameAndUserType(USERNAME, EUserType.B2C)).thenReturn(Optional.of(userOfType(EUserType.B2C)));
        when(passwordEncoder.matches(eq(PASSWORD), any())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest(null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERROR_401_3001);
    }

    private LoginRequest loginRequest(String domain) {
        LoginRequest request = new LoginRequest();
        request.setUsername(USERNAME);
        request.setPassword(PASSWORD);
        request.setDomain(domain);
        return request;
    }
}
