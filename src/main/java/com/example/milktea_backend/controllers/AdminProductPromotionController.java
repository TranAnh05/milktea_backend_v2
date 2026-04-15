package com.example.milktea_backend.controllers;

import com.example.milktea_backend.dtos.requests.AdminProductPromotionRequest;
import com.example.milktea_backend.dtos.responses.AdminProductPromotionResponse;
import com.example.milktea_backend.dtos.responses.ApiResponse;
import com.example.milktea_backend.services.interfaces.IAdminProductPromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/product-promotions")
@RequiredArgsConstructor
public class AdminProductPromotionController {

    private final IAdminProductPromotionService adminProductPromotionService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER','ROLE_ACCOUNTANT')")
    public ResponseEntity<ApiResponse<Page<AdminProductPromotionResponse>>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.<Page<AdminProductPromotionResponse>>builder()
                .data(adminProductPromotionService.getAllPromotions(keyword, categoryId, isActive, page, size))
                .build());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<Integer>> create(@Valid @RequestBody AdminProductPromotionRequest request) {
        int created = adminProductPromotionService.createPromotions(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<Integer>builder()
                        .status(201)
                        .message("Đã tạo " + created + " khuyến mãi sản phẩm")
                        .data(created)
                        .build());
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> toggle(@PathVariable Long id) {
        adminProductPromotionService.togglePromotion(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Đã đổi trạng thái khuyến mãi")
                .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        adminProductPromotionService.deletePromotion(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Đã xóa khuyến mãi")
                .build());
    }
}
