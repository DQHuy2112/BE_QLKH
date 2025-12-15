package com.example.auth_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateUserPermissionsRequest {
    private List<Long> permissionIds;
}

