package com.dumy.module.user.controller;

import com.dumy.common.ApiResponse;
import com.dumy.module.user.dto.UserProfileResponse;
import com.dumy.module.user.service.UserService;
import com.dumy.module.session.web.CurrentUser;
import com.dumy.module.session.web.CurrentUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Session-protected user profile endpoints")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get the logged-in user's own basic identity")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(@CurrentUser CurrentUserPrincipal principal) {
        UserProfileResponse response = userService.getCurrentUser(principal.userId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
