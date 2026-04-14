package com.example.milktea_backend.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminOrderStatusRequest {

    @NotBlank(message = "Trạng thái không được để trống")
    private String orderStatus; // PENDING | CONFIRMED | PREPARING | DELIVERING | COMPLETED | CANCELLED

    private String cancelReason; // Chỉ cần khi status = CANCELLED
}
