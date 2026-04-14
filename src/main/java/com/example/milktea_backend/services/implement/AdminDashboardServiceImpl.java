package com.example.milktea_backend.services.implement;

import com.example.milktea_backend.dtos.responses.AdminDashboardResponse;
import com.example.milktea_backend.enums.OrderStatus;
import com.example.milktea_backend.repositories.OrderRepository;
import com.example.milktea_backend.repositories.ProductRepository;
import com.example.milktea_backend.repositories.UserRepository;
import com.example.milktea_backend.services.interfaces.IAdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements IAdminDashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard(String period, Integer year, String fromDay, String toDay) {

        int targetYear = (year != null) ? year : LocalDate.now().getYear();

        // 1. Tổng quan thẻ KPI
        long totalOrders    = orderRepository.count();
        long completed      = orderRepository.countByOrderStatus(OrderStatus.COMPLETED);
        long pending        = orderRepository.countByOrderStatus(OrderStatus.PENDING);
        long cancelled      = orderRepository.countByOrderStatus(OrderStatus.CANCELLED);
        long totalCustomers = userRepository.countByRoleCode("ROLE_CUSTOMER");
        long totalRevenue   = orderRepository.sumRevenueByDateRange(null, null);

        // 2. Biểu đồ doanh thu theo period
        List<AdminDashboardResponse.RevenuePoint> chart = buildRevenueChart(period, targetYear, fromDay, toDay);

        // 3. Top 5 sản phẩm bán chạy trong khoảng thời gian
        LocalDateTime from = resolveFrom(period, targetYear, fromDay);
        LocalDateTime to   = resolveTo(period, targetYear, toDay);
        List<AdminDashboardResponse.TopProductDto> topProducts = buildTopProducts(from, to);

        // 4. Phân bổ trạng thái đơn
        List<AdminDashboardResponse.OrderStatusCount> statusDist = List.of(
            AdminDashboardResponse.OrderStatusCount.builder().status("PENDING").count(pending).build(),
            AdminDashboardResponse.OrderStatusCount.builder().status("COMPLETED").count(completed).build(),
            AdminDashboardResponse.OrderStatusCount.builder().status("CANCELLED").count(cancelled).build(),
            AdminDashboardResponse.OrderStatusCount.builder()
                .status("CONFIRMING")
                .count(orderRepository.countByOrderStatus(OrderStatus.CONFIRMED))
                .build(),
            AdminDashboardResponse.OrderStatusCount.builder()
                .status("PREPARING")
                .count(orderRepository.countByOrderStatus(OrderStatus.PREPARING))
                .build(),
            AdminDashboardResponse.OrderStatusCount.builder()
                .status("DELIVERING")
                .count(orderRepository.countByOrderStatus(OrderStatus.DELIVERING))
                .build()
        );

        return AdminDashboardResponse.builder()
                .totalOrders(totalOrders)
                .completedOrders(completed)
                .pendingOrders(pending)
                .cancelledOrders(cancelled)
                .totalRevenue(totalRevenue)
                .totalCustomers(totalCustomers)
                .revenueChart(chart)
                .topProducts(topProducts)
                .orderStatusDistribution(statusDist)
                .build();
    }

    // =====================================================================
    //  PRIVATE HELPERS
    // =====================================================================

    private List<AdminDashboardResponse.RevenuePoint> buildRevenueChart(
            String period, int year, String fromDay, String toDay) {

        List<AdminDashboardResponse.RevenuePoint> points = new ArrayList<>();

        switch (period == null ? "month" : period.toLowerCase()) {

            case "day" -> {
                // fromDay..toDay — VD: "2026-04-01" đến "2026-04-30"
                LocalDateTime from = parseDate(fromDay, LocalDate.now().withDayOfMonth(1));
                LocalDateTime to   = parseDate(toDay, LocalDate.now()).plusDays(1).minusNanos(1);
                List<Object[]> rows = orderRepository.revenueByDay(from, to);
                for (Object[] r : rows) {
                    points.add(AdminDashboardResponse.RevenuePoint.builder()
                            .label(r[0].toString())
                            .orderCount(toLong(r[1]))
                            .revenue(toLong(r[2]))
                            .build());
                }
            }

            case "quarter" -> {
                List<Object[]> rows = orderRepository.revenueByQuarter(year);
                for (Object[] r : rows) {
                    points.add(AdminDashboardResponse.RevenuePoint.builder()
                            .label(r[0].toString())
                            .orderCount(toLong(r[1]))
                            .revenue(toLong(r[2]))
                            .build());
                }
            }

            case "year" -> {
                List<Object[]> rows = orderRepository.revenueByYear();
                for (Object[] r : rows) {
                    points.add(AdminDashboardResponse.RevenuePoint.builder()
                            .label(r[0].toString())
                            .orderCount(toLong(r[1]))
                            .revenue(toLong(r[2]))
                            .build());
                }
            }

            default -> {
                // "month" — mặc định
                List<Object[]> rows = orderRepository.revenueByMonth(year);
                for (Object[] r : rows) {
                    points.add(AdminDashboardResponse.RevenuePoint.builder()
                            .label(r[0].toString())
                            .orderCount(toLong(r[1]))
                            .revenue(toLong(r[2]))
                            .build());
                }
            }
        }
        return points;
    }

    private List<AdminDashboardResponse.TopProductDto> buildTopProducts(LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = orderRepository.findTopSellingProducts(from, to, 5);
        List<AdminDashboardResponse.TopProductDto> list = new ArrayList<>();
        for (Object[] r : rows) {
            list.add(AdminDashboardResponse.TopProductDto.builder()
                    .productId(r[0] != null ? ((Number) r[0]).longValue() : null)
                    .productName(r[1] != null ? r[1].toString() : "")
                    .thumbnailUrl(r[2] != null ? r[2].toString() : "")
                    .totalQuantitySold(toLong(r[3]))
                    .totalRevenue(toLong(r[4]))
                    .build());
        }
        return list;
    }

    private LocalDateTime parseDate(String dateStr, LocalDate fallback) {
        if (dateStr == null || dateStr.isBlank()) return fallback.atStartOfDay();
        return LocalDate.parse(dateStr).atStartOfDay();
    }

    private LocalDateTime resolveFrom(String period, int year, String fromDay) {
        if (period == null) return LocalDate.of(year, 1, 1).atStartOfDay();
        return switch (period.toLowerCase()) {
            case "day"     -> parseDate(fromDay, LocalDate.now().withDayOfMonth(1));
            case "quarter", "month" -> LocalDate.of(year, 1, 1).atStartOfDay();
            case "year"    -> null;
            default        -> LocalDate.of(year, 1, 1).atStartOfDay();
        };
    }

    private LocalDateTime resolveTo(String period, int year, String toDay) {
        if (period == null) return LocalDate.of(year, 12, 31).atTime(LocalTime.MAX);
        return switch (period.toLowerCase()) {
            case "day"     -> parseDate(toDay, LocalDate.now()).plusDays(1).minusNanos(1);
            case "quarter", "month" -> LocalDate.of(year, 12, 31).atTime(LocalTime.MAX);
            case "year"    -> null;
            default        -> LocalDate.of(year, 12, 31).atTime(LocalTime.MAX);
        };
    }

    private Long toLong(Object val) {
        if (val == null) return 0L;
        return ((Number) val).longValue();
    }
}
