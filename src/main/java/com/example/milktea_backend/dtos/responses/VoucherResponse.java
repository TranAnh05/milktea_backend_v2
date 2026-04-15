package com.example.milktea_backend.dtos.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VoucherResponse {
    private Long id;
    private String code;
    private String discountType; // PERCENT | FIXED_AMOUNT
    private Integer discountValue; // Giá trị giảm gốc của voucher
    private Integer discountAmount; // Số tiền cụ thể được giảm
    private String message; // Câu chúc mừng (VD: "Đã áp dụng giảm 20%")
    private Integer minOrderAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @JsonProperty("discount_type")
    public String getDiscountTypeSnake() {
        return discountType;
    }

    @JsonProperty("discount_value")
    public Integer getDiscountValueSnake() {
        return discountValue;
    }
}
