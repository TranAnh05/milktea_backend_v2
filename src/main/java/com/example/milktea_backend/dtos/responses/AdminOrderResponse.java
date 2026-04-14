package com.example.milktea_backend.dtos.responses;

import com.example.milktea_backend.enums.OrderStatus;
import com.example.milktea_backend.enums.PaymentMethod;
import com.example.milktea_backend.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminOrderResponse {
    private String orderId;
    private String guestName;
    private String guestPhone;
    private String guestAddress;
    private String note;
    private Integer subTotal;
    private Integer shippingFee;
    private Integer discountAmount;
    private Integer finalTotal;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private OrderStatus orderStatus;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String voucherCode;       // Nullable
    private Long userId;              // Nullable (guest)
    private String userEmail;         // Nullable (guest)
    private Integer totalItemCount;
}
