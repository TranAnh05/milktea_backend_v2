package com.example.milktea_backend.repositories;

import com.example.milktea_backend.entities.ProductSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSizeRepository extends JpaRepository<ProductSize, Long> {

    // ===== PHƯƠNG THỨC CŨ (GIỮ NGUYÊN) =====
    @Query("SELECT ps FROM ProductSize ps WHERE ps.product.id = :productId AND ps.isActive = true")
    List<ProductSize> findActiveSizesByProductId(@Param("productId") Long productId);

    Optional<ProductSize> findByProductIdAndSizeId(Long productId, Long sizeId);

    // ===== PHƯƠNG THỨC MỚI CHO ADMIN =====
    @Modifying
    @Transactional
    @Query("DELETE FROM ProductSize ps WHERE ps.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);
}
