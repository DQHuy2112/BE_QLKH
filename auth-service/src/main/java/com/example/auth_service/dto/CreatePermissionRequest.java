package com.example.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePermissionRequest {
    @NotBlank(message = "Mã quyền không được để trống")
    @Size(min = 2, max = 50, message = "Mã quyền phải có từ 2 đến 50 ký tự")
    @Pattern(regexp = "^[A-Z_]+$", message = "Mã quyền chỉ được chứa chữ cái in hoa và dấu gạch dưới")
    private String permissionCode;

    @NotBlank(message = "Tên hiển thị không được để trống")
    @Size(max = 255, message = "Tên hiển thị không được vượt quá 255 ký tự")
    private String displayName;
}

