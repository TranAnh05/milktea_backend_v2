package com.example.milktea_backend.repositories;

import com.example.milktea_backend.entities.Order;
import com.example.milktea_backend.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    // Tìm tất cả đơn hàng của User, có phân trang
    Page<Order> findByUserId(Long userId, Pageable pageable);

    // Tìm đơn hàng của User kèm theo trạng thái (VD: Chỉ lấy đơn Đang giao), có phân trang
    Page<Order> findByUserIdAndOrderStatus(Long userId, OrderStatus orderStatus, Pageable pageable);

    // Lấy chi tiết đơn hàng (Sẽ dùng để kiểm tra bảo mật)
    Optional<Order> findByIdAndUserId(String id, Long userId);

    // Tra cứu đơn hàng cho Guest (Kết hợp ID và Số điện thoại)
    Optional<Order> findByIdAndGuestPhone(String id, String guestPhone);
}
