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
     *   - year   : 2026  (dùng cho month/quarter)
     *   - from   : "2026-04-01"  (dùng cho period=day)
     *   - to     : "2026-04-30"  (dùng cho period=day)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        AdminDashboardResponse data = dashboardService.getDashboard(period, year, from, to);
        return ResponseEntity.ok(ApiResponse.<AdminDashboardResponse>builder()
                .message("Lấy dữ liệu dashboard thành công")
                .data(data)
                .build());
    }
}
