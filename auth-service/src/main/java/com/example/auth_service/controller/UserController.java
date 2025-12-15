package com.example.auth_service.controller;

import com.example.auth_service.common.ApiResponse;
import com.example.auth_service.dto.CreateUserRequest;
import com.example.auth_service.dto.ResetPasswordRequest;
import com.example.auth_service.dto.UpdateUserRequest;
import com.example.auth_service.dto.UpdateUserPermissionsRequest;
import com.example.auth_service.dto.UserDto;
import com.example.auth_service.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<com.example.auth_service.dto.PageResponse<UserDto>> searchUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long roleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserDto> users = userService.searchUsers(username, email, phone, active, roleId, pageable);
        
        // Convert to PageResponse format
        com.example.auth_service.dto.PageResponse<UserDto> pageResponse = 
            new com.example.auth_service.dto.PageResponse<>(
                users.getContent(),
                users.getTotalElements(),
                users.getTotalPages(),
                users.getNumber(),
                users.getSize()
            );
        
        return ApiResponse.ok(pageResponse);
    }

    @GetMapping("/{id}")
    public ApiResponse<UserDto> getUserById(@PathVariable Long id) {
        UserDto user = userService.getUserById(id);
        return ApiResponse.ok(user);
    }

    @PostMapping
    public ApiResponse<UserDto> createUser(@RequestBody CreateUserRequest request) {
        UserDto user = userService.createUser(request);
        return ApiResponse.ok("Tạo thành viên thành công", user);
    }

    @PutMapping("/{id}")
    public ApiResponse<UserDto> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        UserDto user = userService.updateUser(id, request);
        return ApiResponse.ok("Cập nhật thành viên thành công", user);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.ok("Xóa thành viên thành công");
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<String> resetUserPassword(
            @PathVariable Long id,
            @RequestBody ResetPasswordRequest request
    ) {
        String newPassword = userService.resetUserPassword(id, request);
        return ApiResponse.ok("Đặt lại mật khẩu thành công. Mật khẩu mới: " + newPassword, newPassword);
    }

    @PutMapping("/{id}/permissions")
    public ApiResponse<UserDto> updateUserPermissions(
            @PathVariable Long id,
            @RequestBody UpdateUserPermissionsRequest request
    ) {
        UserDto user = userService.updateUserPermissions(id, request);
        return ApiResponse.ok("Cập nhật quyền thành công", user);
    }
}

