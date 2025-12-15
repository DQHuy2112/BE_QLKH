package com.example.auth_service.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String newPassword;
    private Boolean generateRandomPassword; // If true, generate random password
}

