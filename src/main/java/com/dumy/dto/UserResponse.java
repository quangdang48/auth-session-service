package com.dumy.dto;

import com.dumy.entity.EUserType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private String id;
    private String username;
    private EUserType userType;
    private String tenantId;
    private String tenantName;
    private Instant createdAt;
}
