package com.example.milktea_backend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "size_id", nullable = false)
    private Size size;

    @Column(name = "sugar_level", length = 20)
    @Builder.Default
    private String sugarLevel = "100%";

    @Column(name = "ice_level", length = 20)
    @Builder.Default
    private String iceLevel = "100%";

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    // Chuỗi Hash chống trùng lặp
    @Column(name = "item_signature", nullable = false)
    private String itemSignature;

    @OneToMany(mappedBy = "cartItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default // Thêm cái này để tương thích với @Builder của Lombok
    private List<CartItemTopping> toppings = new ArrayList<>();
}
