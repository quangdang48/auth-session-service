package com.dumy.controller;

import com.dumy.common.ApiResponse;
import com.dumy.dto.LoginRequest;
import com.dumy.dto.LoginResponse;
import com.dumy.dto.RegisterB2BRequest;
import com.dumy.dto.RegisterB2CRequest;
import com.dumy.dto.UserResponse;
import com.dumy.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registration and login endpoints for B2B and B2C users")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/b2c")
    @Operation(summary = "Register a B2C (individual) user")
    public ResponseEntity<ApiResponse<UserResponse>> registerB2C(@Valid @RequestBody RegisterB2CRequest request) {
        UserResponse response = authService.registerB2C(request);
        return new ResponseEntity<>(ApiResponse.success(response), HttpStatus.CREATED);
    }

    @PostMapping("/register/b2b")
    @Operation(summary = "Register a B2B user, creating a new tenant owned by that user")
    public ResponseEntity<ApiResponse<UserResponse>> registerB2B(@Valid @RequestBody RegisterB2BRequest request) {
        UserResponse response = authService.registerB2B(request);
        return new ResponseEntity<>(ApiResponse.success(response), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with username/password; pass `domain` to log into a specific tenant")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the current session")
    public ResponseEntity<ApiResponse<Void>> logout() {
        authService.logout();
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
