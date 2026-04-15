package com.example.milktea_backend.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminProductPromotionRequest {

    @NotBlank
    private String targetType; // ALL | CATEGORY | PRODUCTS

    private List<Long> productIds;

    private List<Long> categoryIds;

    @NotBlank
    private String discountType; // PERCENT | FIXED_AMOUNT

    @NotNull
    @Positive
    private Integer discountValue;

    @NotNull
    private LocalDateTime startDate;

    @NotNull
    private LocalDateTime endDate;

    private Boolean isActive = true;
}
