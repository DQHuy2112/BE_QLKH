package com.example.auth_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateRolePermissionsRequest {
    private List<Long> permissionIds;
}

