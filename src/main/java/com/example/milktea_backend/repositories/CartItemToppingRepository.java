package com.example.milktea_backend.repositories;

import com.example.milktea_backend.dtos.responses.CartToppingResponse;
import com.example.milktea_backend.entities.CartItemTopping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartItemToppingRepository extends JpaRepository<CartItemTopping, Long> {
    List<CartItemTopping> findByCartItemId(Long cartItemId);
}
