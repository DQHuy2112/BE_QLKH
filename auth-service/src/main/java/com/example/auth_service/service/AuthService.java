package com.example.auth_service.service;

import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.LoginResponse;
import com.example.auth_service.dto.UpdateProfileRequest;
import com.example.auth_service.dto.UserProfileDto;

public interface AuthService {
    LoginResponse login(LoginRequest req);
    UserProfileDto getCurrentUserProfile(String username);
    UserProfileDto updateProfile(String username, UpdateProfileRequest request);
    void deleteAccount(String username);
}