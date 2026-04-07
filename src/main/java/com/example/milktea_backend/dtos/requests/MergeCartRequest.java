package com.example.milktea_backend.dtos.requests;

import lombok.Data;
import java.util.List;

@Data
public class MergeCartRequest {
    private List<CartItemRequest> cartItems;
}
