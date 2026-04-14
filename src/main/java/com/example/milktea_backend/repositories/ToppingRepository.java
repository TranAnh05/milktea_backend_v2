package com.example.milktea_backend.repositories;

import com.example.milktea_backend.entities.Topping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ToppingRepository extends JpaRepository<Topping, Long> {

    // ===== PHƯƠNG THỨC CŨ (GIỮ NGUYÊN) =====
    List<Topping> findByIsActiveTrue();

    // ===== PHƯƠNG THỨC MỚI CHO ADMIN =====
    List<Topping> findByIsActiveFalse();
}
