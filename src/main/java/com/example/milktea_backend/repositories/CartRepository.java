package com.example.milktea_backend.repositories;

import com.example.milktea_backend.entities.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    // Tìm giỏ hàng theo User ID
    Optional<Cart> findByUserId(Long userId);
}
