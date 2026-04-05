package com.example.milktea_backend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product; // Dùng để tham chiếu khi cần, không dùng lấy giá

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(name = "product_image")
    private String productImage;

    @Column(name = "size_name", nullable = false, length = 50)
    private String sizeName;

    @Column(name = "sugar_level", nullable = false, length = 20)
    private String sugarLevel;

    @Column(name = "ice_level", nullable = false, length = 20)
    private String iceLevel;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    // Quan hệ 1-N: Khi lưu OrderItem thì lưu luôn OrderItemTopping
    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItemTopping> orderItemToppings = new ArrayList<>();
}
