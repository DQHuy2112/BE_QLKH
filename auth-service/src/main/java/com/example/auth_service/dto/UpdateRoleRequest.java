package com.example.auth_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateRoleRequest {
    private String roleCode;
    private String displayName;
    private List<Long> permissionIds;
}

