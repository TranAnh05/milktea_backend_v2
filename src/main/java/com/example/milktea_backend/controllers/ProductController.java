package com.example.milktea_backend.controllers;

import com.example.milktea_backend.dtos.responses.ApiResponse;
import com.example.milktea_backend.dtos.responses.ProductResponse;
import com.example.milktea_backend.services.interfaces.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final IProductService productService;

    @GetMapping("/promotional")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getPromotionalProducts() {
        List<ProductResponse> products = productService.getPromotionalProducts();

        return ResponseEntity.ok(
                ApiResponse.<List<ProductResponse>>builder()
                        .message("Lấy danh sách sản phẩm khuyến mãi thành công")
                        .data(products)
                        .build()
        );
    }
}
