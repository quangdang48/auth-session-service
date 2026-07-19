package com.dumy.service;

import com.dumy.dto.UserProfileResponse;
import com.dumy.entity.EUserType;
import com.dumy.entity.User;
import com.dumy.exception.BusinessException;
import com.dumy.exception.ErrorCode;
import com.dumy.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserServiceImpl userService;

    private static final String USER_ID = "user-1";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserServiceImpl(userRepository);
    }

    @Test
    void returnsProfileForGivenUser() {
        User user = User.builder().id(USER_ID).username("alice").userType(EUserType.B2C).build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getCurrentUser(USER_ID);

        assertThat(response.getId()).isEqualTo(USER_ID);
        assertThat(response.getUsername()).isEqualTo("alice");
        assertThat(response.getUserType()).isEqualTo(EUserType.B2C);
    }

    @Test
    void throwsWhenUserNoLongerExists() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser(USER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERROR_404_2001);
    }
}
