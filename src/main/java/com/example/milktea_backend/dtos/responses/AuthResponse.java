package com.example.milktea_backend.dtos.responses;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AuthResponse {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private UserDto user;

    // DTO nội bộ chứa thông tin cơ bản trả về cho Frontend
    @Data
    @Builder
    public static class UserDto {
        private Long id;
        private String email;
        private String fullName;
        private String phone;
        private List<String> roles;
    }
}