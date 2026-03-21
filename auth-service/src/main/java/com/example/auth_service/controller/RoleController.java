package com.example.auth_service.controller;

import com.example.auth_service.common.ApiResponse;
import com.example.auth_service.dto.*;
import com.example.auth_service.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MANAGER')")
    public ApiResponse<List<RoleDto>> getAllRoles(
            @RequestParam(required = false) String search
    ) {
        List<RoleDto> roles;
        if (search != null && !search.isEmpty()) {
            roles = roleService.searchRoles(search);
        } else {
            roles = roleService.getAllRoles();
        }
        return ApiResponse.ok(roles);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MANAGER')")
    public ApiResponse<RoleDto> getRoleById(@PathVariable Long id) {
        RoleDto role = roleService.getRoleById(id);
        return ApiResponse.ok(role);
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MANAGER')")
    public ApiResponse<RoleDto> getRoleByCode(@PathVariable String code) {
        RoleDto role = roleService.getRoleByCode(code);
        return ApiResponse.ok(role);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<RoleDto> createRole(@Valid @RequestBody CreateRoleRequest request) {
        RoleDto role = roleService.createRole(request);
        return ApiResponse.ok("Tạo vai trò thành công", role);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<RoleDto> updateRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        RoleDto role = roleService.updateRole(id, request);
        return ApiResponse.ok("Cập nhật vai trò thành công", role);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<String> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ApiResponse.ok("Xóa vai trò thành công");
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<RoleDto> updateRolePermissions(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRolePermissionsRequest request
    ) {
        RoleDto role = roleService.updateRolePermissions(id, request);
        return ApiResponse.ok("Cập nhật phân quyền thành công", role);
    }
}

