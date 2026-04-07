package com.example.milktea_backend.repositories;

import com.example.milktea_backend.entities.ProductPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductPromotionRepository extends JpaRepository<ProductPromotion, Long> {

    // Lấy các khuyến mãi đang Active, và thời gian hiện tại nằm giữa StartDate và EndDate
    // JOIN FETCH giúp lấy luôn thông tin Product trong 1 câu Query (tránh lỗi N+1 Query)
    @Query("SELECT pp FROM ProductPromotion pp JOIN FETCH pp.product p " +
            "WHERE p.isActive = true AND pp.isActive = true " +
            "AND pp.startDate <= :now AND pp.endDate >= :now")
    List<ProductPromotion> findActivePromotions(@Param("now") LocalDateTime now);
}
