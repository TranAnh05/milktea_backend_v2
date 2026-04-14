package com.example.milktea_backend.services.interfaces;

import com.example.milktea_backend.dtos.responses.AdminDashboardResponse;

import java.util.List;

public interface IAdminDashboardService {

    /**
     * Lấy dữ liệu tổng quan dashboard
     * @param period  "day" | "month" | "quarter" | "year"
     * @param year    Năm cần xem (VD: 2026), dùng cho month/quarter
     * @param fromDay VD: "2026-04-01" dùng khi period = "day"
     * @param toDay   VD: "2026-04-30" dùng khi period = "day"
     */
    AdminDashboardResponse getDashboard(String period, Integer year, String fromDay, String toDay);
}
