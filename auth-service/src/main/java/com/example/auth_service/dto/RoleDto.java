package com.example.auth_service.dto;

import com.example.auth_service.entity.AdRole;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class RoleDto {
    private Long id;
    private String roleCode;
    private String displayName;
    private List<PermissionDto> permissions;
    private Date createdAt;
    private Date updatedAt;

    public static RoleDto fromEntity(AdRole r) {
        RoleDto dto = new RoleDto();
        dto.setId(r.getId());
        dto.setRoleCode(r.getRoleCode());
        dto.setDisplayName(r.getDisplayName());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setUpdatedAt(r.getUpdatedAt());
        
        if (r.getPermissions() != null) {
            dto.setPermissions(
                r.getPermissions().stream()
                    .map(PermissionDto::fromEntity)
                    .collect(Collectors.toList())
            );
        }
        
        return dto;
    }
}

