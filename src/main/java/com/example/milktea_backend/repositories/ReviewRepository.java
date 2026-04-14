package com.example.milktea_backend.repositories;

import com.example.milktea_backend.entities.Review;
import com.example.milktea_backend.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Lấy reviews cho admin (filter theo status, product)
    @Query("SELECT r FROM Review r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:productId IS NULL OR r.product.id = :productId)")
    Page<Review> findAllForAdmin(
            @Param("status") ReviewStatus status,
            @Param("productId") Long productId,
            Pageable pageable);
}
