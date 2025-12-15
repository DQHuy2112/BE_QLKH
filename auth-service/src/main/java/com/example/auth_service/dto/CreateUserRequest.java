package com.example.auth_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateUserRequest {
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String province;
    private String district;
    private String ward;
    private String country;
    private Boolean active;
    private List<Long> roleIds;
}

