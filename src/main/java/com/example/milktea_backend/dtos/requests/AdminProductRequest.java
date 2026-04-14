package com.example.milktea_backend.dtos.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AdminProductRequest {

    @NotNull(message = "Category ID không được để trống")
    private Long categoryId;

    @NotBlank(message = "SKU không được để trống")
    private String sku;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Giá gốc không được để trống")
    @Min(value = 0, message = "Giá gốc phải >= 0")
    private Integer basePrice;

    private String thumbnailUrl;

    private Boolean isActive = true;

    // Danh sách size đi kèm khi tạo/sửa sản phẩm
    private List<ProductSizeEntry> sizes;

    @Data
    public static class ProductSizeEntry {
        private Long sizeId;
        private Integer priceSurcharge;
        private Boolean isActive = true;
    }
}
