package com.example.milktea_backend.dtos.responses;

import com.example.milktea_backend.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderHistoryResponse {
    private String orderId;
    private Integer finalTotal;
    private OrderStatus orderStatus;
    private LocalDateTime createdAt;

    // Tối ưu UX: Hiển thị tên 1 món đại diện + số lượng các món khác
    private String firstItemName;
    private String firstItemImage;
    private int totalItemCount; // Tổng số ly trong đơn
}
