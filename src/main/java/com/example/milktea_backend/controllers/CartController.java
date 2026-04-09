package com.example.milktea_backend.controllers;

import com.example.milktea_backend.dtos.requests.CartItemRequest;
import com.example.milktea_backend.dtos.requests.MergeCartRequest;
import com.example.milktea_backend.dtos.responses.ApiResponse;
import com.example.milktea_backend.dtos.responses.CartResponse;
import com.example.milktea_backend.security.CustomUserDetails;
import com.example.milktea_backend.services.interfaces.ICartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final ICartService cartService;

    // API 1: Thêm món vào giỏ
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<Void>> addCartItem(@RequestBody CartItemRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();

        cartService.addOrUpdateCartItem(userId, request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Đã thêm vào giỏ hàng thành công")
                .build());
    }

    // API 2: Gộp giỏ hàng (Merge)
    @PostMapping("/merge")
    public ResponseEntity<ApiResponse<Void>> mergeCart(@RequestBody MergeCartRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();

        cartService.mergeCart(userId, request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Đã đồng bộ giỏ hàng thành công")
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();
        CartResponse cartResponse = cartService.getCart(userId);

        return ResponseEntity.ok(ApiResponse.<CartResponse>builder()
                .message("Lấy giỏ hàng thành công")
                .data(cartResponse)
                .build());
    }

    @PutMapping("/items")
    public ResponseEntity<ApiResponse<Void>> updateQuantity(
            @RequestParam String signature,
            @RequestParam Integer quantity,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();
        cartService.updateCartItemQuantity(userId, signature, quantity);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Cập nhật số lượng thành công")
                .build());
    }

    // API 5: Xóa món khỏi giỏ hàng
    @DeleteMapping("/items")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @RequestParam String signature,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();
        cartService.removeCartItem(userId, signature);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Đã xóa món khỏi giỏ hàng")
                .build());
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();
        cartService.clearCart(userId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Đã làm sạch giỏ hàng")
                .build());
    }
}
