package com.example.auth_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateRoleRequest {
    private String roleCode;
    private String displayName;
    private List<Long> permissionIds;
}

