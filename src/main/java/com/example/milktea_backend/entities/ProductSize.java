package com.example.milktea_backend.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_sizes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"product_id", "size_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSize extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "size_id", nullable = false)
    private Size size;

    @Column(name = "price_surcharge", nullable = false)
    @Builder.Default
    private Integer priceSurcharge = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
