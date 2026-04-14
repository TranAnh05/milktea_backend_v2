package com.example.milktea_backend.repositories;

import com.example.milktea_backend.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// ============================================================
// EXTEND UserRepository — GIỮ NGUYÊN PHƯƠNG THỨC CŨ
// ============================================================
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ===== PHƯƠNG THỨC CŨ =====
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // ===== PHƯƠNG THỨC MỚI CHO ADMIN =====

    // Lấy tất cả user với filter
    @Query("SELECT u FROM User u WHERE " +
           "(:keyword IS NULL OR u.email LIKE %:keyword% OR u.fullName LIKE %:keyword% OR u.phone LIKE %:keyword%) AND " +
           "(:isActive IS NULL OR u.isActive = :isActive)")
    Page<User> findAllForAdmin(
            @Param("keyword") String keyword,
            @Param("isActive") Boolean isActive,
            Pageable pageable);

    // Đếm số user theo role code (dùng cho dashboard)
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.code = :roleCode")
    Long countByRoleCode(@Param("roleCode") String roleCode);

    // Đếm tổng đơn hàng của một user
    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    Long countOrdersByUserId(@Param("userId") Long userId);
}
