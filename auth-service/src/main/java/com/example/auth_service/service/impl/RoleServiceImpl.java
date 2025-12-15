package com.example.auth_service.service.impl;

import com.example.auth_service.dto.*;
import com.example.auth_service.entity.AdPermission;
import com.example.auth_service.entity.AdRole;
import com.example.auth_service.exception.NotFoundException;
import com.example.auth_service.exception.DuplicateException;
import com.example.auth_service.repository.AdPermissionRepository;
import com.example.auth_service.repository.AdRoleRepository;
import com.example.auth_service.service.RoleService;
import com.example.auth_service.util.ActivityLogHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements RoleService {

    private final AdRoleRepository roleRepository;
    private final AdPermissionRepository permissionRepository;
    private final ActivityLogHelper activityLogHelper;

    public RoleServiceImpl(AdRoleRepository roleRepository, AdPermissionRepository permissionRepository, ActivityLogHelper activityLogHelper) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.activityLogHelper = activityLogHelper;
    }

    @Override
    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(RoleDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<RoleDto> searchRoles(String search) {
        return roleRepository.searchRoles(search).stream()
                .map(RoleDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public RoleDto getRoleById(Long id) {
        AdRole role = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Role not found with id: " + id));
        return RoleDto.fromEntity(role);
    }

    @Override
    public RoleDto getRoleByCode(String code) {
        AdRole role = roleRepository.findByRoleCode(code)
                .orElseThrow(() -> new NotFoundException("Role not found with code: " + code));
        return RoleDto.fromEntity(role);
    }

    @Override
    @Transactional
    public RoleDto createRole(CreateRoleRequest request) {
        // Check if role code already exists
        if (roleRepository.findByRoleCode(request.getRoleCode()).isPresent()) {
            throw new DuplicateException("Mã vai trò đã tồn tại: " + request.getRoleCode());
        }

        AdRole role = new AdRole();
        role.setRoleCode(request.getRoleCode());
        role.setDisplayName(request.getDisplayName());
        role.setCreatedAt(new Date());
        role.setUpdatedAt(new Date());

        // Set permissions if provided
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            Set<AdPermission> permissions = new HashSet<>();
            for (Long permissionId : request.getPermissionIds()) {
                permissionRepository.findById(permissionId).ifPresent(permissions::add);
            }
            role.setPermissions(permissions);
        }

        AdRole savedRole = roleRepository.save(role);
        
        // Log activity
        activityLogHelper.logActivity(
            "CREATE_ROLE",
            "ROLE",
            savedRole.getId(),
            savedRole.getRoleCode(),
            String.format("Created new role: %s", savedRole.getRoleCode())
        );
        
        return RoleDto.fromEntity(savedRole);
    }

    @Override
    @Transactional
    public RoleDto updateRole(Long id, UpdateRoleRequest request) {
        AdRole role = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Role not found with id: " + id));

        if (request.getRoleCode() != null) {
            // Check if new role code already exists (excluding current role)
            roleRepository.findByRoleCode(request.getRoleCode())
                    .ifPresent(existingRole -> {
                        if (!existingRole.getId().equals(id)) {
                            throw new DuplicateException("Mã vai trò đã tồn tại: " + request.getRoleCode());
                        }
                    });
            role.setRoleCode(request.getRoleCode());
        }

        if (request.getDisplayName() != null) {
            role.setDisplayName(request.getDisplayName());
        }

        // Update permissions if provided
        if (request.getPermissionIds() != null) {
            Set<AdPermission> permissions = new HashSet<>();
            for (Long permissionId : request.getPermissionIds()) {
                permissionRepository.findById(permissionId).ifPresent(permissions::add);
            }
            role.setPermissions(permissions);
        }

        role.setUpdatedAt(new Date());
        AdRole updatedRole = roleRepository.save(role);
        
        // Log activity
        activityLogHelper.logActivity(
            "UPDATE_ROLE",
            "ROLE",
            updatedRole.getId(),
            updatedRole.getRoleCode(),
            String.format("Updated role: %s", updatedRole.getRoleCode())
        );
        
        return RoleDto.fromEntity(updatedRole);
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        AdRole role = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Role not found with id: " + id));
        
        String roleCode = role.getRoleCode();
        roleRepository.delete(role);
        
        // Log activity
        activityLogHelper.logActivity(
            "DELETE_ROLE",
            "ROLE",
            id,
            roleCode,
            String.format("Deleted role: %s", roleCode)
        );
    }

    @Override
    @Transactional
    public RoleDto updateRolePermissions(Long roleId, UpdateRolePermissionsRequest request) {
        AdRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found with id: " + roleId));

        Set<AdPermission> permissions = new HashSet<>();
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            for (Long permissionId : request.getPermissionIds()) {
                permissionRepository.findById(permissionId).ifPresent(permissions::add);
            }
        }
        role.setPermissions(permissions);
        role.setUpdatedAt(new Date());

        AdRole updatedRole = roleRepository.save(role);
        
        // Log activity
        activityLogHelper.logActivity(
            "UPDATE_ROLE_PERMISSIONS",
            "ROLE",
            updatedRole.getId(),
            updatedRole.getRoleCode(),
            String.format("Updated permissions for role: %s", updatedRole.getRoleCode())
        );
        
        return RoleDto.fromEntity(updatedRole);
    }
}

