package com.example.milktea_backend.dtos.responses;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminUserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private Boolean isVerified;
    private Boolean isActive;
    private List<String> roles;      // ["ROLE_ADMIN", "ROLE_STAFF"]
    private LocalDateTime createdAt;
    private Long totalOrders;        // Số đơn đã đặt
}
