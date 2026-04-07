package com.example.milktea_backend.dtos.responses;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SizeDto {
    private Long id; // ID của bảng sizes
    private String name; // VD: "Nhỏ (Size S)", "Lớn (Size L)"
    private Integer priceSurcharge; // Phụ thu: 0, 5000, 10000...
}
