package com.example.milktea_backend.dtos.responses;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VoucherResponse {
    private Long id;
    private String code;
    private Integer discountAmount; // Số tiền cụ thể được giảm
    private String message; // Câu chúc mừng (VD: "Đã áp dụng giảm 20%")
    private Integer minOrderAmount;
}
