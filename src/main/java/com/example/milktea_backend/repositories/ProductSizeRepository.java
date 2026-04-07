package com.example.milktea_backend.repositories;

import com.example.milktea_backend.entities.ProductSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductSizeRepository extends JpaRepository<ProductSize, Long> {
    @Query("SELECT ps FROM ProductSize ps JOIN FETCH ps.size s " +
            "WHERE ps.product.id = :productId AND ps.isActive = true")
    List<ProductSize> findActiveSizesByProductId(@Param("productId") Long productId);
}
