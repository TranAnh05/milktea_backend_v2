package com.example.milktea_backend.controllers;

import com.example.milktea_backend.dtos.responses.ApiResponse;
import com.example.milktea_backend.dtos.responses.ProductDetailResponse;
import com.example.milktea_backend.dtos.responses.ProductResponse;
import com.example.milktea_backend.services.interfaces.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(@PathVariable String slug) {
        ProductDetailResponse detail = productService.getProductDetail(slug);

        return ResponseEntity.ok(
                ApiResponse.<ProductDetailResponse>builder()
                        .message("Lấy chi tiết sản phẩm thành công")
                        .data(detail)
                        .build()
        );
    }

    @GetMapping("/category/{slug}")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProductsByCategorySlug(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Page<ProductResponse> products = productService.getProductsByCategorySlug(slug, page, size);

        return ResponseEntity.ok(ApiResponse.<Page<ProductResponse>>builder()
                .message("Lấy danh sách sản phẩm thành công")
                .data(products)
                .build());
    }
}
