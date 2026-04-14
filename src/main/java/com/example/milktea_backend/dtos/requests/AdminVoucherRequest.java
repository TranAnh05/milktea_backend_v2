package com.example.milktea_backend.dtos.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminVoucherRequest {

    @NotBlank(message = "Mã voucher không được để trống")
    private String code;

    @NotBlank(message = "Loại giảm giá không được để trống")
    private String discountType; // PERCENT | FIXED_AMOUNT

    @NotNull
    @Min(value = 1, message = "Giá trị giảm phải > 0")
    private Integer discountValue;

    private Integer minOrderAmount = 0;
    private Integer maxDiscountAmount; // Nullable

    @NotNull
    @Min(value = 1, message = "Số lượng phải > 0")
    private Integer quantity;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime endDate;

    private Boolean isActive = true;
}
