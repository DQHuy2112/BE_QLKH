package com.example.auth_service.service;

import com.example.auth_service.dto.CreateUserRequest;
import com.example.auth_service.dto.ResetPasswordRequest;
import com.example.auth_service.dto.UpdateUserRequest;
import com.example.auth_service.dto.UpdateUserPermissionsRequest;
import com.example.auth_service.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    Page<UserDto> searchUsers(String username, String email, String phone, Boolean active, Long roleId, Pageable pageable);
    UserDto getUserById(Long id);
    UserDto createUser(CreateUserRequest request);
    UserDto updateUser(Long id, UpdateUserRequest request);
    void deleteUser(Long id);
    String resetUserPassword(Long id, ResetPasswordRequest request);
    UserDto updateUserPermissions(Long id, UpdateUserPermissionsRequest request);
}

