package com.example.milktea_backend.repositories;

import com.example.milktea_backend.entities.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    // Tìm xem trong Giỏ hàng này, đã có ly nước nào cấu hình y hệt chưa?
    Optional<CartItem> findByCartIdAndItemSignature(Long cartId, String itemSignature);
    List<CartItem> findByCartId(Long cartId);
}
