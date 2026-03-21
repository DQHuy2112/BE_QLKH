package com.example.auth_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UpdateUserPermissionsRequest {
    @NotNull(message = "Danh sách quyền không được để trống")
    private List<Long> permissionIds;
}

