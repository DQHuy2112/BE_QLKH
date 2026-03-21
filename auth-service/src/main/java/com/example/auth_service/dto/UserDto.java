package com.example.auth_service.dto;

import com.example.auth_service.entity.AdUser;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class UserDto {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String avatar;
    private String address;
    private String province;
    private String district;
    private String ward;
    private String country;
    private Boolean active;
    private List<String> roles;
    private List<String> permissions;
    private Date createdAt;
    private Date updatedAt;

    public static UserDto fromEntity(AdUser u) {
        UserDto dto = new UserDto();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setFirstName(u.getFirstName());
        dto.setLastName(u.getLastName());
        dto.setFullName(
                (u.getFirstName() != null ? u.getFirstName() : "") + " " +
                        (u.getLastName() != null ? u.getLastName() : "")
        );
        dto.setEmail(u.getEmail());
        dto.setPhone(u.getPhone());
        dto.setAvatar(u.getAvatar());
        dto.setAddress(u.getAddress());
        dto.setProvince(u.getProvince());
        dto.setDistrict(u.getDistrict());
        dto.setWard(u.getWard());
        dto.setCountry(u.getCountry());
        dto.setActive(u.getActive());
        dto.setCreatedAt(u.getCreatedAt());
        dto.setUpdatedAt(u.getUpdatedAt());

        dto.setRoles(
                u.getRoles() != null
                        ? u.getRoles().stream().map(r -> r.getRoleCode()).collect(Collectors.toList())
                        : List.of()
        );
        dto.setPermissions(
                u.getPermissions() != null
                        ? u.getPermissions().stream().map(p -> p.getPermissionCode()).collect(Collectors.toList())
                        : List.of()
        );
        return dto;
    }
}
