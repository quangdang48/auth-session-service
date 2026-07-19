package com.dumy.module.user.dto;

import com.dumy.module.user.entity.EUserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserProfileResponse {

    private String id;
    private String username;
    private EUserType userType;
}
