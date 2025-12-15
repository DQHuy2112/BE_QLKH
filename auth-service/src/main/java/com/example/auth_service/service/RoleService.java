package com.example.auth_service.service;

import com.example.auth_service.dto.*;

import java.util.List;

public interface RoleService {
    List<RoleDto> getAllRoles();
    List<RoleDto> searchRoles(String search);
    RoleDto getRoleById(Long id);
    RoleDto getRoleByCode(String code);
    RoleDto createRole(CreateRoleRequest request);
    RoleDto updateRole(Long id, UpdateRoleRequest request);
    void deleteRole(Long id);
    RoleDto updateRolePermissions(Long roleId, UpdateRolePermissionsRequest request);
}

