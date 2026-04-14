package com.example.milktea_backend.repositories;

import com.example.milktea_backend.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // ===== PHƯƠNG THỨC CŨ (GIỮ NGUYÊN) =====
    Optional<Category> findBySlug(String slug);
    List<Category> findByIsActiveTrue();

    // ===== PHƯƠNG THỨC MỚI CHO ADMIN =====
    List<Category> findByIsActive(Boolean isActive);
    Optional<Category> findByName(String name);
    boolean existsBySlug(String slug);
}
