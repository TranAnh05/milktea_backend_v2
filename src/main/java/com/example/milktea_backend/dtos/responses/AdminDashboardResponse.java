package com.example.milktea_backend.dtos.responses;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminDashboardResponse {

    // Tổng quan
    private Long totalOrders;
    private Long completedOrders;
    private Long pendingOrders;
    private Long cancelledOrders;
    private Long totalRevenue;       // Tổng doanh thu (finalTotal của đơn COMPLETED)
    private Long totalCustomers;     // Số user có ROLE_CUSTOMER

    // Biểu đồ doanh thu theo thời gian (ngày/tuần/tháng/quý/năm)
    private List<RevenuePoint> revenueChart;

    // Top sản phẩm bán chạy
    private List<TopProductDto> topProducts;

    // Phân bổ trạng thái đơn hàng
    private List<OrderStatusCount> orderStatusDistribution;

    @Data
    @Builder
    public static class RevenuePoint {
        private String label;   // VD: "01/04", "Tháng 4", "Q1/2026"
        private Long revenue;
        private Long orderCount;
    }

    @Data
    @Builder
    public static class TopProductDto {
        private Long productId;
        private String productName;
        private String thumbnailUrl;
        private Long totalQuantitySold;
        private Long totalRevenue;
    }

    @Data
    @Builder
    public static class OrderStatusCount {
        private String status;
        private Long count;
    }
}
