package com.example.milktea_backend.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminCategoryRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    private String name;

    private String description;
    private String imageUrl;
    private Boolean isActive = true;
}
