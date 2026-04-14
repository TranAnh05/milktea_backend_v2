package com.example.milktea_backend.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class AdminUserRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    private String password; // Nullable khi update (chỉ bắt buộc khi create)

    @NotBlank(message = "Tên không được để trống")
    private String fullName;

    private String phone;
    private Boolean isActive = true;
    private List<String> roleCodes; // VD: ["ROLE_STAFF", "ROLE_ACCOUNTANT"]
}
