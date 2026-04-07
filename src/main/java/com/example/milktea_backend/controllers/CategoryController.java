package com.example.milktea_backend.controllers;

import com.example.milktea_backend.dtos.responses.ApiResponse;
import com.example.milktea_backend.dtos.responses.CategoryResponse;
import com.example.milktea_backend.services.interfaces.ICategoryService;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final ICategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getActiveCategories() {
        List<CategoryResponse> categories = categoryService.getActiveCategories();

        return ResponseEntity.ok(
                ApiResponse.<List<CategoryResponse>>builder()
                        .message("Lấy danh sách danh mục thành công")
                        .data(categories)
                        .build()
        );
    }
}
