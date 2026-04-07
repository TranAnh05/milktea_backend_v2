package com.example.milktea_backend.dtos.responses;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CartResponse {
    private List<CartItemResponse> cartItems;
    private Integer cartCount;
    private Integer cartTotal;
}
