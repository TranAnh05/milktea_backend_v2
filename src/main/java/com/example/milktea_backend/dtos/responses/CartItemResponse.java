package com.example.milktea_backend.dtos.responses;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CartItemResponse {
    private String signature;
    private Long productId;
    private String slug;
    private String productName;
    private String thumbnailUrl;
    private Long sizeId;
    private String sizeName;
    private String sugarLevel;
    private String iceLevel;
    private Integer unitPrice;
    private Integer quantity;
    private Integer totalPrice;
    private List<CartToppingResponse> toppings;
}
