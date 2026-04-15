package com.example.milktea_backend.repositories;

import com.example.milktea_backend.entities.ProductPromotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductPromotionRepository extends JpaRepository<ProductPromotion, Long> {

    // Lấy các khuyến mãi đang Active, và thời gian hiện tại nằm giữa StartDate và EndDate
    // JOIN FETCH giúp lấy luôn thông tin Product trong 1 câu Query (tránh lỗi N+1 Query)
    @Query("SELECT pp FROM ProductPromotion pp JOIN FETCH pp.product p " +
            "WHERE p.isActive = true AND pp.isActive = true " +
            "AND pp.startDate <= :now AND pp.endDate >= :now")
    List<ProductPromotion> findActivePromotions(@Param("now") LocalDateTime now);

    @Query("SELECT pp FROM ProductPromotion pp " +
            "WHERE pp.product.id = :productId AND pp.isActive = true " +
            "AND pp.startDate <= :now AND pp.endDate >= :now")
    Optional<ProductPromotion> findActivePromotionByProductId(@Param("productId") Long productId, @Param("now") LocalDateTime now);

    @Query("SELECT pp FROM ProductPromotion pp JOIN pp.product p JOIN p.category c WHERE " +
            "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:categoryId IS NULL OR c.id = :categoryId) AND " +
            "(:isActive IS NULL OR pp.isActive = :isActive)")
    Page<ProductPromotion> findAllForAdmin(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );

    List<ProductPromotion> findByProductIdInAndIsActiveTrue(Collection<Long> productIds);

    @Query("SELECT pp FROM ProductPromotion pp WHERE pp.product.id IN :productIds AND pp.isActive = true " +
            "AND pp.startDate <= :endDate AND pp.endDate >= :startDate")
    List<ProductPromotion> findOverlappingActivePromotions(
            @Param("productIds") Collection<Long> productIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
