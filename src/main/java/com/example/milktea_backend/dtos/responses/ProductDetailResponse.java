package com.example.milktea_backend.dtos.responses;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ProductDetailResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String thumbnailUrl;
    private Float averageRating;
    private Integer reviewCount;
    private String categoryName;
    private String categorySlug;

    private Integer originalPrice;
    private Integer promotionalPrice; // Sẽ bằng originalPrice nếu không có KM
    private Integer discountPercent;

    // Các danh sách con đi kèm
    private List<SizeDto> sizes;
    private List<ToppingDto> toppings;
}
