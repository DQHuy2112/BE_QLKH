package com.example.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateUserRequest {
    @Size(min = 3, max = 50, message = "Tên đăng nhập phải có từ 3 đến 50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Tên đăng nhập chỉ được chứa chữ cái, số và dấu gạch dưới")
    private String username;

    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    private String firstName;
    private String lastName;

    @Email(message = "Email không hợp lệ")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    private String email;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại không hợp lệ (10-11 chữ số)")
    private String phone;

    private String address;
    private String province;
    private String district;
    private String ward;
    private String country;
    private Boolean active;
    private List<Long> roleIds;
}

