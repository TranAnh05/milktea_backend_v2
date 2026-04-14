package com.example.milktea_backend.controllers;

import com.example.milktea_backend.dtos.requests.AdminVoucherRequest;
import com.example.milktea_backend.dtos.responses.ApiResponse;
import com.example.milktea_backend.dtos.responses.VoucherResponse;
import com.example.milktea_backend.services.interfaces.IAdminProductService;
import com.example.milktea_backend.services.interfaces.IAdminVoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/vouchers")
@RequiredArgsConstructor
public class AdminVoucherController {

    private final IAdminVoucherService adminVoucherService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER','ROLE_ACCOUNTANT')")
    public ResponseEntity<ApiResponse<Page<VoucherResponse>>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.<Page<VoucherResponse>>builder()
                .data(adminVoucherService.getAllVouchers(keyword, isActive, page, size)).build());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<VoucherResponse>> create(
            @Valid @RequestBody AdminVoucherRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.<VoucherResponse>builder()
                .status(201).message("Tạo voucher thành công")
                .data(adminVoucherService.createVoucher(request)).build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<VoucherResponse>> update(
            @PathVariable Long id, @Valid @RequestBody AdminVoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.<VoucherResponse>builder()
                .message("Cập nhật voucher thành công")
                .data(adminVoucherService.updateVoucher(id, request)).build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        adminVoucherService.deleteVoucher(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder().message("Đã xóa voucher").build());
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> toggle(@PathVariable Long id) {
        adminVoucherService.toggleVoucherStatus(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder().message("Đã đổi trạng thái voucher").build());
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER','ROLE_ACCOUNTANT')")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "excel") String format) {
        byte[] data = adminVoucherService.exportVouchers(format);
        String fn = "voucher." + ("csv".equalsIgnoreCase(format) ? "csv" : "xlsx");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fn + "\"")
                .contentType("csv".equalsIgnoreCase(format)
                        ? MediaType.parseMediaType("text/csv; charset=UTF-8")
                        : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    /** Template CSV: Mã voucher | Loại giảm | Giá trị giảm | Số lượng | Bắt đầu | Kết thúc */
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<IAdminProductService.ImportResult>> importData(
            @RequestParam("file") MultipartFile file) {
        IAdminProductService.ImportResult result = adminVoucherService.importVouchers(file);
        return ResponseEntity.ok(ApiResponse.<IAdminProductService.ImportResult>builder()
                .message(String.format("Import: %d thành công, %d thất bại", result.success(), result.failed()))
                .data(result).build());
    }
}
