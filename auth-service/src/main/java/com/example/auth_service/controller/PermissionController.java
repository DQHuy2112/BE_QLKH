package com.example.auth_service.controller;

import com.example.auth_service.common.ApiResponse;
import com.example.auth_service.dto.CreatePermissionRequest;
import com.example.auth_service.dto.PermissionDto;
import com.example.auth_service.dto.UpdatePermissionRequest;
import com.example.auth_service.service.PermissionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    public ApiResponse<List<PermissionDto>> getAllPermissions(
            @RequestParam(required = false) String search
    ) {
        List<PermissionDto> permissions;
        if (search != null && !search.isEmpty()) {
            permissions = permissionService.searchPermissions(search);
        } else {
            permissions = permissionService.getAllPermissions();
        }
        return ApiResponse.ok(permissions);
    }

    @GetMapping("/{id}")
    public ApiResponse<PermissionDto> getPermissionById(@PathVariable Long id) {
        PermissionDto permission = permissionService.getPermissionById(id);
        return ApiResponse.ok(permission);
    }

    @GetMapping("/code/{code}")
    public ApiResponse<PermissionDto> getPermissionByCode(@PathVariable String code) {
        PermissionDto permission = permissionService.getPermissionByCode(code);
        return ApiResponse.ok(permission);
    }

    @PostMapping
    public ApiResponse<PermissionDto> createPermission(@RequestBody CreatePermissionRequest request) {
        PermissionDto permission = permissionService.createPermission(request);
        return ApiResponse.ok("Tạo quyền thành công", permission);
    }

    @PutMapping("/{id}")
    public ApiResponse<PermissionDto> updatePermission(@PathVariable Long id, @RequestBody UpdatePermissionRequest request) {
        PermissionDto permission = permissionService.updatePermission(id, request);
        return ApiResponse.ok("Cập nhật quyền thành công", permission);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return ApiResponse.ok("Xóa quyền thành công");
    }
}

