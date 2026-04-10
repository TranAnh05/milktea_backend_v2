package com.example.milktea_backend.entities;

import com.example.milktea_backend.enums.OrderStatus;
import com.example.milktea_backend.enums.PaymentMethod;
import com.example.milktea_backend.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Id
    @Column(length = 50)
    private String id; // ID tự sinh dạng Chuỗi (Vd: ORD-12345)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // Nullable cho Khách vãng lai

    @Column(name = "guest_name", nullable = false, length = 100)
    private String guestName;

    @Column(name = "guest_phone", nullable = false, length = 20)
    private String guestPhone;

    @Column(name = "guest_address", nullable = false, columnDefinition = "TEXT")
    private String guestAddress;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "sub_total", nullable = false)
    private Integer subTotal;

    @Column(name = "shipping_fee")
    @Builder.Default
    private Integer shippingFee = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(name = "discount_amount")
    @Builder.Default
    private Integer discountAmount = 0;

    @Column(name = "final_total", nullable = false)
    private Integer finalTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status")
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    // Quan hệ 1-N: Khi lưu Order thì lưu luôn OrderItem
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();
}