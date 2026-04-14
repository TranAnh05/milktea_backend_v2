package com.example.milktea_backend.controllers;

import com.example.milktea_backend.dtos.requests.OrderRequest;
import com.example.milktea_backend.dtos.responses.ApiResponse;
import com.example.milktea_backend.dtos.responses.OrderDetailResponse;
import com.example.milktea_backend.dtos.responses.OrderHistoryResponse;
import com.example.milktea_backend.dtos.responses.PlaceOrderResponse;
import com.example.milktea_backend.entities.Order;
import com.example.milktea_backend.enums.OrderStatus;
import com.example.milktea_backend.security.CustomUserDetails;
import com.example.milktea_backend.services.interfaces.IOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;

    // API: Đặt hàng (Dùng chung cho cả User và Guest)
    @PostMapping
    public ResponseEntity<ApiResponse<PlaceOrderResponse>> placeOrder(
            @Valid @RequestBody OrderRequest request, // Thêm @Valid để kích hoạt kiểm tra dữ liệu
            @AuthenticationPrincipal CustomUserDetails userDetails) { // Chấp nhận null nếu không có Token

        // Kiểm tra xem ai đang gọi API: Có Token -> lấy ID, Không có -> null
        Long userId = (userDetails != null) ? userDetails.getUser().getId() : null;

        // Chuyền xuống Service xử lý và nhận lại mã đơn hàng
        PlaceOrderResponse response = orderService.placeOrder(userId, request);

        return ResponseEntity.ok(ApiResponse.<PlaceOrderResponse>builder()
                .message("Đặt hàng thành công")
                .data(response) // Trả mã đơn hàng (Vd: ORD-1698765432) về cho Frontend
                .build());
    }

    // 1. Lấy danh sách lịch sử
    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<Page<OrderHistoryResponse>>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Bắt buộc đăng nhập
        if (userDetails == null) throw new AccessDeniedException("Vui lòng đăng nhập");

        Page<OrderHistoryResponse> result = orderService.getMyOrders(userDetails.getUser().getId(), status, page, size);
        return ResponseEntity.ok(ApiResponse.<Page<OrderHistoryResponse>>builder()
                .data(result)
                .build());
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderDetail( // Đổi type ở đây
            @PathVariable String orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) throw new AccessDeniedException("Vui lòng đăng nhập");

        return ResponseEntity.ok(ApiResponse.<OrderDetailResponse>builder()
                .message("Lấy chi tiết đơn hàng thành công")
                .data(orderService.getOrderDetail(userDetails.getUser().getId(), orderId))
                .build());
    }

    // 3. Hủy đơn hàng (Nhận thêm body chứa lý do hủy)
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @PathVariable String orderId,
            @RequestBody(required = false) java.util.Map<String, String> body, // Lấy lý do hủy từ JSON { "reason": "Đổi ý" }
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) throw new AccessDeniedException("Vui lòng đăng nhập");

        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : null;
        orderService.cancelOrder(userDetails.getUser().getId(), orderId, reason);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Đã hủy đơn hàng thành công")
                .build());
    }

    // API Tra cứu đơn hàng cho Guest (Không cần Token)
    // Ví dụ: GET /api/v1/orders/track?orderId=ORD-123&phone=0901234567
    @GetMapping("/track")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> trackGuestOrder(
            @RequestParam String orderId,
            @RequestParam String phone) {

        return ResponseEntity.ok(ApiResponse.<OrderDetailResponse>builder()
                .message("Tra cứu đơn hàng thành công")
                .data(orderService.trackGuestOrder(orderId, phone))
                .build());
    }

    @GetMapping("/{orderId}/payment-status")
    public ResponseEntity<ApiResponse<String>> checkPaymentStatus(@PathVariable String orderId) {
        String status = orderService.checkPaymentStatus(orderId);

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .message("Lấy trạng thái thanh toán thành công")
                .data(status)
                .build());
    }
}
