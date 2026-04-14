package com.example.milktea_backend.dtos.responses;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminProductResponse {
    private Long id;
    private String sku;
    private String name;
    private String slug;
    private String description;
    private Integer basePrice;
    private String thumbnailUrl;
    private Float averageRating;
    private Integer reviewCount;
    private Boolean isActive;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SizeDto> sizes;
}
