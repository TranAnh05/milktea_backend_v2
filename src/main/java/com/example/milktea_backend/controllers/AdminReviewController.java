package com.example.milktea_backend.controllers;

import com.example.milktea_backend.dtos.responses.AdminReviewResponse;
import com.example.milktea_backend.dtos.responses.ApiResponse;
import com.example.milktea_backend.enums.ReviewStatus;
import com.example.milktea_backend.services.interfaces.IAdminReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final IAdminReviewService adminReviewService;

    // ---------------------------------------------------------------
    // GET ALL — ADMIN, MANAGER
    // ---------------------------------------------------------------
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<Page<AdminReviewResponse>>> getAll(
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) Long productId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(ApiResponse.<Page<AdminReviewResponse>>builder()
                .data(adminReviewService.getAllReviews(status, productId, page, size))
                .build());
    }

    // ---------------------------------------------------------------
    // APPROVE
    // ---------------------------------------------------------------
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> approve(@PathVariable Long id) {
        adminReviewService.approveReview(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Đã duyệt đánh giá")
                .build());
    }

    // ---------------------------------------------------------------
    // HIDE
    // ---------------------------------------------------------------
    @PatchMapping("/{id}/hide")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> hide(@PathVariable Long id) {
        adminReviewService.hideReview(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Đã ẩn đánh giá")
                .build());
    }

    // ---------------------------------------------------------------
    // DELETE — Chỉ ADMIN
    // ---------------------------------------------------------------
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        adminReviewService.deleteReview(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Đã xóa đánh giá")
                .build());
    }
}
