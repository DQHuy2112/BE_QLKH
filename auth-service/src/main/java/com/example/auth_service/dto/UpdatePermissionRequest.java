package com.example.auth_service.dto;

import lombok.Data;

@Data
public class UpdatePermissionRequest {
    private String permissionCode;
    private String displayName;
}

