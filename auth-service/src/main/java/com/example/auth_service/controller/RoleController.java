package com.example.auth_service.controller;

import com.example.auth_service.common.ApiResponse;
import com.example.auth_service.dto.*;
import com.example.auth_service.service.RoleService;
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
    public ApiResponse<RoleDto> getRoleById(@PathVariable Long id) {
        RoleDto role = roleService.getRoleById(id);
        return ApiResponse.ok(role);
    }

    @GetMapping("/code/{code}")
    public ApiResponse<RoleDto> getRoleByCode(@PathVariable String code) {
        RoleDto role = roleService.getRoleByCode(code);
        return ApiResponse.ok(role);
    }

    @PostMapping
    public ApiResponse<RoleDto> createRole(@RequestBody CreateRoleRequest request) {
        RoleDto role = roleService.createRole(request);
        return ApiResponse.ok("Tạo vai trò thành công", role);
    }

    @PutMapping("/{id}")
    public ApiResponse<RoleDto> updateRole(@PathVariable Long id, @RequestBody UpdateRoleRequest request) {
        RoleDto role = roleService.updateRole(id, request);
        return ApiResponse.ok("Cập nhật vai trò thành công", role);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ApiResponse.ok("Xóa vai trò thành công");
    }

    @PutMapping("/{id}/permissions")
    public ApiResponse<RoleDto> updateRolePermissions(
            @PathVariable Long id,
            @RequestBody UpdateRolePermissionsRequest request
    ) {
        RoleDto role = roleService.updateRolePermissions(id, request);
        return ApiResponse.ok("Cập nhật phân quyền thành công", role);
    }
}

