package com.example.milktea_backend.controllers;

import com.example.milktea_backend.dtos.requests.AdminOrderStatusRequest;
import com.example.milktea_backend.dtos.responses.AdminOrderResponse;
import com.example.milktea_backend.dtos.responses.ApiResponse;
import com.example.milktea_backend.dtos.responses.OrderDetailResponse;
import com.example.milktea_backend.enums.OrderStatus;
import com.example.milktea_backend.services.interfaces.IAdminOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final IAdminOrderService adminOrderService;

    // ---------------------------------------------------------------
    // GET ALL — ADMIN, MANAGER, STAFF, ACCOUNTANT (chỉ đọc)
    // ---------------------------------------------------------------
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER','ROLE_STAFF','ROLE_ACCOUNTANT')")
    public ResponseEntity<ApiResponse<Page<AdminOrderResponse>>> getAllOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "10")  int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Page<AdminOrderResponse> result = adminOrderService.getAllOrders(
                keyword, status, from, to, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.<Page<AdminOrderResponse>>builder()
                .data(result).build());
    }

    // ---------------------------------------------------------------
    // GET DETAIL
    // ---------------------------------------------------------------
    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER','ROLE_STAFF','ROLE_ACCOUNTANT')")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderDetail(@PathVariable String orderId) {
        return ResponseEntity.ok(ApiResponse.<OrderDetailResponse>builder()
                .data(adminOrderService.getOrderDetail(orderId)).build());
    }

    // ---------------------------------------------------------------
    // UPDATE STATUS — ADMIN, MANAGER, STAFF
    // ---------------------------------------------------------------
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER','ROLE_STAFF')")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable String orderId,
            @Valid @RequestBody AdminOrderStatusRequest request) {

        adminOrderService.updateOrderStatus(orderId, request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Cập nhật trạng thái đơn hàng thành công").build());
    }

    // ---------------------------------------------------------------
    // EXPORT — ADMIN, MANAGER, ACCOUNTANT
    // ---------------------------------------------------------------
    /**
     * GET /api/v1/admin/orders/export?format=excel&from=...&to=...&status=COMPLETED
     * format: "excel" (default) | "csv"
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER','ROLE_ACCOUNTANT')")
    public ResponseEntity<byte[]> exportOrders(
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) OrderStatus status) {

        byte[] data = adminOrderService.exportOrders(from, to, status, format);

        String filename = "don-hang." + ("csv".equalsIgnoreCase(format) ? "csv" : "xlsx");
        String contentType = "csv".equalsIgnoreCase(format)
                ? "text/csv; charset=UTF-8"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }
}
