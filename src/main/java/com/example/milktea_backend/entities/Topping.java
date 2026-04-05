package com.example.milktea_backend.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "toppings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Topping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private Integer price = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
