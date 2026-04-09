package com.example.milktea_backend.dtos.responses;

import com.example.milktea_backend.enums.OrderStatus;
import com.example.milktea_backend.enums.PaymentMethod;
import com.example.milktea_backend.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderDetailResponse {
    // 1. Thông tin chung của đơn
    private String orderId;
    private LocalDateTime createdAt;
    private OrderStatus orderStatus;
    private String cancelReason;

    // 2. Thông tin giao hàng
    private String guestName;
    private String guestPhone;
    private String guestAddress;
    private String note;

    // 3. Thông tin thanh toán
    private Integer subTotal;
    private Integer shippingFee;
    private Integer discountAmount;
    private Integer finalTotal;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;

    // 4. Danh sách món ăn
    private List<OrderItemDto> items;

    // --- CÁC CLASS CON BÊN TRONG ---
    @Data
    @Builder
    public static class OrderItemDto {
        private Long id;
        private Long productId;
        private String productName;
        private String productImage;
        private String sizeName;
        private String sugarLevel;
        private String iceLevel;
        private Integer unitPrice;
        private Integer quantity;
        private Integer totalPrice;
        private List<OrderItemToppingDto> toppings;
    }

    @Data
    @Builder
    public static class OrderItemToppingDto {
        private Long id;
        private String toppingName;
        private Integer toppingPrice;
    }
}
