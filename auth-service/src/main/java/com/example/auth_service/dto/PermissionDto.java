package com.example.auth_service.dto;

import com.example.auth_service.entity.AdPermission;
import lombok.Data;

import java.util.Date;

@Data
public class PermissionDto {
    private Long id;
    private String permissionCode;
    private String displayName;
    private Date createdAt;
    private Date updatedAt;

    public static PermissionDto fromEntity(AdPermission p) {
        PermissionDto dto = new PermissionDto();
        dto.setId(p.getId());
        dto.setPermissionCode(p.getPermissionCode());
        dto.setDisplayName(p.getDisplayName());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        return dto;
    }
}

