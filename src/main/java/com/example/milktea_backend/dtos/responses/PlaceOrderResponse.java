package com.example.milktea_backend.dtos.responses;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlaceOrderResponse {
    private String orderId;
    private Integer finalTotal;
    private String paymentMethod;
}
