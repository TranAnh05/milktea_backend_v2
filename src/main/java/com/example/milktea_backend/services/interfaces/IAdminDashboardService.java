package com.example.milktea_backend.services.interfaces;

import com.example.milktea_backend.dtos.responses.AdminDashboardResponse;


public interface IAdminDashboardService {

    /**
     * Lấy dữ liệu tổng quan dashboard
     * @param period  "day" | "month" | "quarter" | "year"
     * @param year    Năm cần xem (VD: 2026), dùng cho month/quarter/year
     * @param month   Tháng cần xem (1..12), dùng cho period = "month"
     * @param quarter Quý cần xem (1..4), dùng cho period = "quarter"
     * @param date    Ngày cần xem (VD: "2026-04-15"), dùng khi period = "day"
     * @param fromDay VD: "2026-04-01" dùng khi period = "day"
     * @param toDay   VD: "2026-04-30" dùng khi period = "day"
     */
    AdminDashboardResponse getDashboard(String period, Integer year, Integer month, Integer quarter,
                                        String date, String fromDay, String toDay);
}
