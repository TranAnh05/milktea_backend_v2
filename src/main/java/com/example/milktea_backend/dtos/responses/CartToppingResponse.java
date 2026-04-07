package com.example.milktea_backend.dtos.responses;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartToppingResponse {
    private Long id;
    private String name;
    private Integer price;
}
