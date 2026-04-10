package com.example.milktea_backend.controllers;

import com.example.milktea_backend.dtos.responses.ApiResponse;
import com.example.milktea_backend.dtos.responses.VoucherResponse;
import com.example.milktea_backend.services.interfaces.IVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final IVoucherService voucherService;

    // API: Kiểm tra mã giảm giá
    // Ví dụ: GET /api/v1/vouchers/check?code=WELCOME20&orderValue=60000
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<VoucherResponse>> checkVoucher(
            @RequestParam String code,
            @RequestParam Integer orderValue) {

        VoucherResponse response = voucherService.checkVoucher(code, orderValue);

        return ResponseEntity.ok(ApiResponse.<VoucherResponse>builder()
                .message("Hợp lệ")
                .data(response)
                .build());
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<VoucherResponse>>> getActiveVouchers() {
        return ResponseEntity.ok(ApiResponse.<List<VoucherResponse>>builder()
                .message("Lấy danh sách thành công")
                .data(voucherService.getActiveVouchers())
                .build());
    }
}
