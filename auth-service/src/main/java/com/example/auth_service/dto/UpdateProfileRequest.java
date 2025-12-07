package com.example.auth_service.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String province;
    private String district;
    private String ward;
    private String country;
    private String avatar;
}

