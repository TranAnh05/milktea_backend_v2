package com.example.milktea_backend.services.interfaces;

import com.example.milktea_backend.dtos.requests.CartItemRequest;
import com.example.milktea_backend.dtos.requests.MergeCartRequest;
import com.example.milktea_backend.dtos.responses.CartResponse;

public interface ICartService {
    // Thêm hoặc cập nhật số lượng món vào giỏ
    void addOrUpdateCartItem(Long userId, CartItemRequest request);

    // Gộp giỏ hàng từ LocalStorage lên Database
    void mergeCart(Long userId, MergeCartRequest request);

    CartResponse getCart(Long userId);

    void updateCartItemQuantity(Long userId, String signature, Integer quantity);

    void removeCartItem(Long userId, String signature);

    void clearCart(Long userId);
}
