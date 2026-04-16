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
    public AdminDashboardResponse getDashboard(String period, Integer year, Integer month, Integer quarter,
                                               String date, String fromDay, String toDay) {

        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        TimeRange range = resolveRange(period, year, month, quarter, date, fromDay, toDay);

        // 1. Tổng quan thẻ KPI
        long totalOrders    = orderRepository.countByDateRange(range.from(), range.to());
        long completed      = orderRepository.countByOrderStatusAndDateRange(OrderStatus.COMPLETED, range.from(), range.to());
        long pending        = orderRepository.countByOrderStatusAndDateRange(OrderStatus.PENDING, range.from(), range.to());
        long cancelled      = orderRepository.countByOrderStatusAndDateRange(OrderStatus.CANCELLED, range.from(), range.to());
        long totalCustomers = userRepository.countByRoleCode("ROLE_CUSTOMER");
        long totalRevenue   = orderRepository.sumRevenueByDateRange(range.from(), range.to());

        // 2. Biểu đồ doanh thu theo period
        List<AdminDashboardResponse.RevenuePoint> chart =
                buildRevenueChart(period, year, month, quarter, date, fromDay, toDay);

        // 3. Top 5 sản phẩm bán chạy trong khoảng thời gian
        List<AdminDashboardResponse.TopProductDto> topProducts = buildTopProducts(range.from(), range.to());

        // 4. Phân bổ trạng thái đơn
        List<AdminDashboardResponse.OrderStatusCount> statusDist = List.of(
            AdminDashboardResponse.OrderStatusCount.builder().status("PENDING").count(pending).build(),
            AdminDashboardResponse.OrderStatusCount.builder().status("COMPLETED").count(completed).build(),
            AdminDashboardResponse.OrderStatusCount.builder().status("CANCELLED").count(cancelled).build(),
            AdminDashboardResponse.OrderStatusCount.builder()
                .status("CONFIRMING")
                .count(orderRepository.countByOrderStatusAndDateRange(OrderStatus.CONFIRMED, range.from(), range.to()))
                .build(),
            AdminDashboardResponse.OrderStatusCount.builder()
                .status("PREPARING")
                .count(orderRepository.countByOrderStatusAndDateRange(OrderStatus.PREPARING, range.from(), range.to()))
                .build(),
            AdminDashboardResponse.OrderStatusCount.builder()
                .status("DELIVERING")
                .count(orderRepository.countByOrderStatusAndDateRange(OrderStatus.DELIVERING, range.from(), range.to()))
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
            String period, Integer year, Integer month, Integer quarter,
            String date, String fromDay, String toDay) {

        List<AdminDashboardResponse.RevenuePoint> points = new ArrayList<>();
        TimeRange range = resolveRange(period, year, month, quarter, date, fromDay, toDay);
        String normalizedPeriod = normalizePeriod(period);
        int targetYear = (year != null) ? year : LocalDate.now().getYear();

        switch (normalizedPeriod) {

            case "day" -> {
                List<Object[]> rows = orderRepository.revenueByDay(range.from(), range.to());
                for (Object[] r : rows) {
                    points.add(AdminDashboardResponse.RevenuePoint.builder()
                            .label(r[0].toString())
                            .orderCount(toLong(r[1]))
                            .revenue(toLong(r[2]))
                            .build());
                }
            }

            case "quarter" -> {
                List<Object[]> rows = (quarter != null)
                        ? orderRepository.revenueByMonthInRange(range.from(), range.to())
                        : orderRepository.revenueByQuarter(targetYear);
                for (Object[] r : rows) {
                    points.add(AdminDashboardResponse.RevenuePoint.builder()
                            .label(r[0].toString())
                            .orderCount(toLong(r[1]))
                            .revenue(toLong(r[2]))
                            .build());
                }
            }

            case "year" -> {
                List<Object[]> rows = (range.from() != null && range.to() != null)
                        ? orderRepository.revenueByMonth(targetYear)
                        : orderRepository.revenueByYear();
                for (Object[] r : rows) {
                    points.add(AdminDashboardResponse.RevenuePoint.builder()
                            .label(r[0].toString())
                            .orderCount(toLong(r[1]))
                            .revenue(toLong(r[2]))
                            .build());
                }
            }

            default -> {
                List<Object[]> rows = (month != null)
                        ? orderRepository.revenueByDay(range.from(), range.to())
                        : orderRepository.revenueByMonth(targetYear);
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

    private TimeRange resolveRange(String period, Integer year, Integer month, Integer quarter,
                                   String date, String fromDay, String toDay) {
        String normalizedPeriod = normalizePeriod(period);
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        return switch (normalizedPeriod) {
            case "day" -> {
                if (date != null && !date.isBlank()) {
                    LocalDate d = LocalDate.parse(date);
                    yield new TimeRange(d.atStartOfDay(), d.atTime(LocalTime.MAX));
                }
                LocalDateTime from = parseDate(fromDay, LocalDate.now().withDayOfMonth(1));
                LocalDateTime to = parseDate(toDay, LocalDate.now()).plusDays(1).minusNanos(1);
                yield new TimeRange(from, to);
            }
            case "month" -> {
                if (month != null) {
                    validateMonth(month);
                    LocalDate start = LocalDate.of(targetYear, month, 1);
                    yield new TimeRange(start.atStartOfDay(), start.withDayOfMonth(start.lengthOfMonth()).atTime(LocalTime.MAX));
                }
                yield new TimeRange(LocalDate.of(targetYear, 1, 1).atStartOfDay(), LocalDate.of(targetYear, 12, 31).atTime(LocalTime.MAX));
            }
            case "quarter" -> {
                if (quarter != null) {
                    validateQuarter(quarter);
                    int startMonth = (quarter - 1) * 3 + 1;
                    LocalDate start = LocalDate.of(targetYear, startMonth, 1);
                    LocalDate end = start.plusMonths(2).withDayOfMonth(start.plusMonths(2).lengthOfMonth());
                    yield new TimeRange(start.atStartOfDay(), end.atTime(LocalTime.MAX));
                }
                yield new TimeRange(LocalDate.of(targetYear, 1, 1).atStartOfDay(), LocalDate.of(targetYear, 12, 31).atTime(LocalTime.MAX));
            }
            case "year" -> {
                if (year != null) {
                    yield new TimeRange(LocalDate.of(targetYear, 1, 1).atStartOfDay(), LocalDate.of(targetYear, 12, 31).atTime(LocalTime.MAX));
                }
                yield new TimeRange(null, null);
            }
            default -> new TimeRange(null, null);
        };
    }

    private String normalizePeriod(String period) {
        return (period == null || period.isBlank()) ? "month" : period.toLowerCase();
    }

    private void validateMonth(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month phải nằm trong khoảng 1..12");
        }
    }

    private void validateQuarter(int quarter) {
        if (quarter < 1 || quarter > 4) {
            throw new IllegalArgumentException("quarter phải nằm trong khoảng 1..4");
        }
    }

    private Long toLong(Object val) {
        if (val == null) return 0L;
        return ((Number) val).longValue();
    }

    private record TimeRange(LocalDateTime from, LocalDateTime to) {
    }
}
