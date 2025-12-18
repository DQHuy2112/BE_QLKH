package com.example.auth_service.service.impl;

import com.example.auth_service.dto.CreatePermissionRequest;
import com.example.auth_service.dto.PermissionDto;
import com.example.auth_service.dto.UpdatePermissionRequest;
import com.example.auth_service.entity.AdPermission;
import com.example.auth_service.exception.NotFoundException;
import com.example.auth_service.exception.DuplicateException;
import com.example.auth_service.repository.AdPermissionRepository;
import com.example.auth_service.service.PermissionService;
import com.example.auth_service.util.ActivityLogHelper;
import com.example.auth_service.util.ChangeLogUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl implements PermissionService {

    private final AdPermissionRepository permissionRepository;
    private final ActivityLogHelper activityLogHelper;

    public PermissionServiceImpl(AdPermissionRepository permissionRepository, ActivityLogHelper activityLogHelper) {
        this.permissionRepository = permissionRepository;
        this.activityLogHelper = activityLogHelper;
    }

    @Override
    public List<PermissionDto> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(PermissionDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionDto> searchPermissions(String search) {
        return permissionRepository.searchPermissions(search).stream()
                .map(PermissionDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public PermissionDto getPermissionById(Long id) {
        AdPermission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Permission not found with id: " + id));
        return PermissionDto.fromEntity(permission);
    }

    @Override
    public PermissionDto getPermissionByCode(String code) {
        AdPermission permission = permissionRepository.findByPermissionCode(code)
                .orElseThrow(() -> new NotFoundException("Permission not found with code: " + code));
        return PermissionDto.fromEntity(permission);
    }

    @Override
    @Transactional
    public PermissionDto createPermission(CreatePermissionRequest request) {
        // Check if permission code already exists
        if (permissionRepository.findByPermissionCode(request.getPermissionCode()).isPresent()) {
            throw new DuplicateException("Mã quyền đã tồn tại: " + request.getPermissionCode());
        }

        AdPermission permission = new AdPermission();
        permission.setPermissionCode(request.getPermissionCode());
        permission.setDisplayName(request.getDisplayName());
        permission.setCreatedAt(new Date());
        permission.setUpdatedAt(new Date());

        AdPermission savedPermission = permissionRepository.save(permission);
        
        // Log activity
        activityLogHelper.logActivity(
            "CREATE_PERMISSION",
            "PERMISSION",
            savedPermission.getId(),
            savedPermission.getPermissionCode(),
            String.format("Created new permission: %s", savedPermission.getPermissionCode())
        );
        
        return PermissionDto.fromEntity(savedPermission);
    }

    @Override
    @Transactional
    public PermissionDto updatePermission(Long id, UpdatePermissionRequest request) {
        AdPermission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Permission not found with id: " + id));

        // Snapshot trước khi cập nhật để log before/after
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("permissionCode", permission.getPermissionCode());
        before.put("displayName", permission.getDisplayName());

        if (request.getPermissionCode() != null) {
            // Check if new permission code already exists (excluding current permission)
            permissionRepository.findByPermissionCode(request.getPermissionCode())
                    .ifPresent(existingPermission -> {
                        if (!existingPermission.getId().equals(id)) {
                            throw new DuplicateException("Mã quyền đã tồn tại: " + request.getPermissionCode());
                        }
                    });
            permission.setPermissionCode(request.getPermissionCode());
        }

        if (request.getDisplayName() != null) {
            permission.setDisplayName(request.getDisplayName());
        }

        permission.setUpdatedAt(new Date());
        AdPermission updatedPermission = permissionRepository.save(permission);

        // Snapshot sau khi cập nhật
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("permissionCode", updatedPermission.getPermissionCode());
        after.put("displayName", updatedPermission.getDisplayName());

        String details = ChangeLogUtils.buildChangeDetails(before, after);

        // Log activity với chi tiết before/after
        activityLogHelper.logActivity(
                "UPDATE_PERMISSION",
                "PERMISSION",
                updatedPermission.getId(),
                updatedPermission.getPermissionCode(),
                details
        );
        
        return PermissionDto.fromEntity(updatedPermission);
    }

    @Override
    @Transactional
    public void deletePermission(Long id) {
        AdPermission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Permission not found with id: " + id));
        
        String permissionCode = permission.getPermissionCode();
        permissionRepository.delete(permission);
        
        // Log activity
        activityLogHelper.logActivity(
            "DELETE_PERMISSION",
            "PERMISSION",
            id,
            permissionCode,
            String.format("Deleted permission: %s", permissionCode)
        );
    }
}

