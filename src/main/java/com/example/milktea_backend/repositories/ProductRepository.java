package com.example.milktea_backend.repositories;

import com.example.milktea_backend.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySlugAndIsActiveTrue(String slug);

    // Lấy sản phẩm theo Category Slug (Có phân trang)
    @Query("SELECT p FROM Product p WHERE p.category.slug = :slug AND p.isActive = true")
    Page<Product> findActiveProductsByCategorySlug(@Param("slug") String slug, Pageable pageable);

    Page<Product> findByIsActiveTrue(Pageable pageable);
}
