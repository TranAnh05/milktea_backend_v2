package com.example.milktea_backend.dtos.responses;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminProductPromotionResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Long categoryId;
    private String categoryName;
    private Integer basePrice;
    private String discountType;
    private Integer discountValue;
    private Integer promotionalPrice;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
