package com.example.milktea_backend.repositories;

import com.example.milktea_backend.entities.Order;
import com.example.milktea_backend.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// ============================================================
// EXTEND OrderRepository hiện tại — KHÔNG XÓA PHƯƠNG THỨC CŨ
// File này thay thế OrderRepository.java hiện có,
// chỉ bổ sung thêm các query dành cho admin.
// ============================================================
@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    // ===== CÁC PHƯƠNG THỨC CŨ (GIỮ NGUYÊN CHO CLIENT) =====

    Page<Order> findByUserId(Long userId, Pageable pageable);
    Page<Order> findByUserIdAndOrderStatus(Long userId, OrderStatus orderStatus, Pageable pageable);
    Optional<Order> findByIdAndUserId(String id, Long userId);
    Optional<Order> findByIdAndGuestPhone(String id, String guestPhone);

    // ===== PHƯƠNG THỨC MỚI CHO ADMIN =====

    // Lấy tất cả đơn, có filter theo status và khoảng thời gian
    @Query("SELECT o FROM Order o WHERE " +
           "(:status IS NULL OR o.orderStatus = :status) AND " +
           "(:from IS NULL OR o.createdAt >= :from) AND " +
           "(:to IS NULL OR o.createdAt <= :to) AND " +
           "(:keyword IS NULL OR o.id LIKE %:keyword% OR o.guestName LIKE %:keyword% OR o.guestPhone LIKE %:keyword%)")
    Page<Order> findAllForAdmin(
            @Param("status") OrderStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("keyword") String keyword,
            Pageable pageable);

    // Tổng doanh thu theo khoảng thời gian (chỉ đơn COMPLETED)
    @Query("SELECT COALESCE(SUM(o.finalTotal), 0) FROM Order o WHERE " +
           "o.orderStatus = 'COMPLETED' AND " +
           "(:from IS NULL OR o.createdAt >= :from) AND " +
           "(:to IS NULL OR o.createdAt <= :to)")
    Long sumRevenueByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Đếm đơn hàng theo trạng thái
    Long countByOrderStatus(OrderStatus status);

    // Đếm đơn hàng trong khoảng thời gian
    Long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    // Doanh thu theo từng ngày (dùng cho biểu đồ ngày)
    @Query(value = "SELECT DATE(created_at) as label, COUNT(*) as orderCount, SUM(final_total) as revenue " +
                   "FROM orders WHERE order_status = 'COMPLETED' " +
                   "AND created_at BETWEEN :from AND :to " +
                   "GROUP BY DATE(created_at) ORDER BY label",
           nativeQuery = true)
    List<Object[]> revenueByDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Doanh thu theo từng tháng
    @Query(value = "SELECT DATE_FORMAT(MIN(created_at), '%m/%Y') as label, COUNT(*) as orderCount, SUM(final_total) as revenue " +
                   "FROM orders WHERE order_status = 'COMPLETED' " +
                   "AND YEAR(created_at) = :year " +
                   "GROUP BY YEAR(created_at), MONTH(created_at) ORDER BY YEAR(created_at), MONTH(created_at)",
           nativeQuery = true)
    List<Object[]> revenueByMonth(@Param("year") int year);

    // Doanh thu theo quý
    @Query(value = "SELECT CONCAT('Q', QUARTER(created_at), '/', YEAR(created_at)) as label, " +
                   "COUNT(*) as orderCount, SUM(final_total) as revenue " +
                   "FROM orders WHERE order_status = 'COMPLETED' " +
                   "AND YEAR(created_at) = :year " +
                   "GROUP BY QUARTER(created_at) ORDER BY QUARTER(created_at)",
           nativeQuery = true)
    List<Object[]> revenueByQuarter(@Param("year") int year);

    // Doanh thu theo năm
    @Query(value = "SELECT YEAR(created_at) as label, COUNT(*) as orderCount, SUM(final_total) as revenue " +
                   "FROM orders WHERE order_status = 'COMPLETED' " +
                   "GROUP BY YEAR(created_at) ORDER BY YEAR(created_at)",
           nativeQuery = true)
    List<Object[]> revenueByYear();

    // Top sản phẩm bán chạy
    @Query(value = "SELECT oi.product_id, oi.product_name, oi.product_image, " +
                   "SUM(oi.quantity) as totalQty, SUM(oi.total_price) as totalRevenue " +
                   "FROM order_items oi " +
                   "JOIN orders o ON oi.order_id = o.id " +
                   "WHERE o.order_status = 'COMPLETED' " +
                   "AND (:from IS NULL OR o.created_at >= :from) " +
                   "AND (:to IS NULL OR o.created_at <= :to) " +
                   "GROUP BY oi.product_id, oi.product_name, oi.product_image " +
                   "ORDER BY totalQty DESC LIMIT :limit",
           nativeQuery = true)
    List<Object[]> findTopSellingProducts(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("limit") int limit);

    // Đơn hàng cần export theo khoảng thời gian
    @Query("SELECT o FROM Order o WHERE " +
           "(:from IS NULL OR o.createdAt >= :from) AND " +
           "(:to IS NULL OR o.createdAt <= :to) AND " +
           "(:status IS NULL OR o.orderStatus = :status) " +
           "ORDER BY o.createdAt DESC")
    List<Order> findAllForExport(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") OrderStatus status);
}
