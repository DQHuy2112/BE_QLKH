package com.example.auth_service.controller;

import com.example.auth_service.common.ApiResponse;
import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.LoginResponse;
import com.example.auth_service.dto.UpdateProfileRequest;
import com.example.auth_service.dto.UserProfileDto;
import com.example.auth_service.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse res = authService.login(request);
        return ApiResponse.ok("Login success", res);
    }

    @GetMapping("/profile")
    public ApiResponse<UserProfileDto> getProfile() {
        String username = getCurrentUsername();
        UserProfileDto profile = authService.getCurrentUserProfile(username);
        return ApiResponse.ok(profile);
    }

    @PutMapping("/profile")
    public ApiResponse<UserProfileDto> updateProfile(@RequestBody UpdateProfileRequest request) {
        String username = getCurrentUsername();
        UserProfileDto profile = authService.updateProfile(username, request);
        return ApiResponse.ok("Cập nhật thông tin thành công", profile);
    }

    @DeleteMapping("/profile")
    public ApiResponse<String> deleteAccount() {
        String username = getCurrentUsername();
        authService.deleteAccount(username);
        return ApiResponse.ok("Đã vô hiệu hóa tài khoản");
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            return auth.getName();
        }
        throw new RuntimeException("User not authenticated");
    }
}