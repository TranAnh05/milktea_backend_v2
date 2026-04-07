package com.example.milktea_backend.repositories;

import com.example.milktea_backend.entities.Topping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ToppingRepository extends JpaRepository<Topping, Long> {
    List<Topping> findByIsActiveTrue();
}
