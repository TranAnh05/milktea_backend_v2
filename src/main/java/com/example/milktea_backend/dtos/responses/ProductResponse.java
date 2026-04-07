package com.example.milktea_backend.dtos.responses;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String slug;
    private String thumbnailUrl;
    private Float averageRating;

    // Phần quan trọng nhất cho UI:
    private Integer originalPrice;    // Giá gốc (Ví dụ: 50000)
    private Integer promotionalPrice; // Giá sau khi giảm (Ví dụ: 40000)
    private Integer discountPercent;  // Tính sẵn % để hiển thị badge đỏ (Ví dụ: 20)
}
