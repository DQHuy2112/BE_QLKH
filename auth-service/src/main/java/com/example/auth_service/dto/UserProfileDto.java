package com.example.auth_service.dto;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class UserProfileDto {
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
    private Date createdAt;
    private Date updatedAt;

    public static UserProfileDto fromEntity(com.example.auth_service.entity.AdUser user) {
        UserProfileDto dto = new UserProfileDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + " " +
            (user.getLastName() != null ? user.getLastName() : "")).trim();
        dto.setFullName(fullName.isEmpty() ? user.getUsername() : fullName);
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAvatar(user.getAvatar());
        dto.setAddress(user.getAddress());
        dto.setProvince(user.getProvince());
        dto.setDistrict(user.getDistrict());
        dto.setWard(user.getWard());
        dto.setCountry(user.getCountry());
        dto.setActive(user.getActive());
        dto.setRoles(
            user.getRoles() != null
                ? user.getRoles().stream().map(r -> r.getRoleCode() != null ? r.getRoleCode() : "").filter(s -> !s.isEmpty()).toList()
                : List.of()
        );
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}

