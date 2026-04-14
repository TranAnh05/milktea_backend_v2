package com.example.milktea_backend.repositories;

import com.example.milktea_backend.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// ============================================================
// EXTEND ProductRepository — KHÔNG XÓA PHƯƠNG THỨC CŨ
// ============================================================
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // ===== PHƯƠNG THỨC CŨ (GIỮ NGUYÊN) =====
    Optional<Product> findBySlugAndIsActiveTrue(String slug);

    @Query("SELECT p FROM Product p WHERE p.category.slug = :slug AND p.isActive = true")
    Page<Product> findActiveProductsByCategorySlug(@Param("slug") String slug, Pageable pageable);

    Page<Product> findByIsActiveTrue(Pageable pageable);

    // ===== PHƯƠNG THỨC MỚI CHO ADMIN =====

    // Lấy TẤT CẢ sản phẩm (kể cả đã ẩn), có filter keyword + category
    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR p.name LIKE %:keyword% OR p.sku LIKE %:keyword%) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:isActive IS NULL OR p.isActive = :isActive)")
    Page<Product> findAllForAdmin(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("isActive") Boolean isActive,
            Pageable pageable);

    // Kiểm tra SKU trùng (ngoại trừ chính nó khi update)
    boolean existsBySkuAndIdNot(String sku, Long id);
    boolean existsBySku(String sku);
    Optional<Product> findBySku(String sku);

    // Kiểm tra slug trùng
    boolean existsBySlugAndIdNot(String slug, Long id);
    boolean existsBySlug(String slug);

    // Đếm tổng sản phẩm đang hoạt động
    Long countByIsActiveTrue();
}
