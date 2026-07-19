package com.dumy.mapper;

import com.dumy.dto.UserResponse;
import com.dumy.entity.Tenant;
import com.dumy.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user, Tenant tenant) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .userType(user.getUserType())
                .tenantId(tenant != null ? tenant.getId() : null)
                .tenantName(tenant != null ? tenant.getName() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
