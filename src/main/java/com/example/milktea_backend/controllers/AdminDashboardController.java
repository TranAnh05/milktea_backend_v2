package com.example.milktea_backend.controllers;

import com.example.milktea_backend.dtos.responses.AdminDashboardResponse;
import com.example.milktea_backend.dtos.responses.ApiResponse;
import com.example.milktea_backend.services.interfaces.IAdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_ACCOUNTANT')")
public class AdminDashboardController {

    private final IAdminDashboardService dashboardService;

    /**
     * GET /api/v1/admin/dashboard
     * Query params:
     *   - period : "day" | "month" | "quarter" | "year"  (default: "month")
     *   - year   : 2026  (dùng cho month/quarter/year)
     *   - month  : 1..12 (dùng cho period=month)
     *   - quarter: 1..4  (dùng cho period=quarter)
     *   - date   : "2026-04-15" (dùng cho period=day)
     *   - from   : "2026-04-01"  (dùng cho period=day)
     *   - to     : "2026-04-30"  (dùng cho period=day)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer quarter,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        AdminDashboardResponse data = dashboardService.getDashboard(period, year, month, quarter, date, from, to);
        return ResponseEntity.ok(ApiResponse.<AdminDashboardResponse>builder()
                .message("Lấy dữ liệu dashboard thành công")
                .data(data)
                .build());
    }
}
