package com.dumy.service;

import com.dumy.dto.UserProfileResponse;
import com.dumy.entity.User;
import com.dumy.exception.BusinessException;
import com.dumy.exception.ErrorCode;
import com.dumy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserProfileResponse getCurrentUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERROR_404_2001));

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .userType(user.getUserType())
                .build();
    }
}
