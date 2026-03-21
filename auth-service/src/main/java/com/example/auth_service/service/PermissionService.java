package com.example.auth_service.service;

import com.example.auth_service.dto.CreatePermissionRequest;
import com.example.auth_service.dto.PermissionDto;
import com.example.auth_service.dto.UpdatePermissionRequest;

import java.util.List;

public interface PermissionService {
    List<PermissionDto> getAllPermissions();
    List<PermissionDto> searchPermissions(String search);
    PermissionDto getPermissionById(Long id);
    PermissionDto getPermissionByCode(String code);
    PermissionDto createPermission(CreatePermissionRequest request);
    PermissionDto updatePermission(Long id, UpdatePermissionRequest request);
    void deletePermission(Long id);
}

