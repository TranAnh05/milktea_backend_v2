package com.example.milktea_backend.controllers;

import com.example.milktea_backend.dtos.requests.AdminProductRequest;
import com.example.milktea_backend.dtos.responses.AdminProductResponse;
import com.example.milktea_backend.dtos.responses.ApiResponse;
import com.example.milktea_backend.services.interfaces.IAdminProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final IAdminProductService adminProductService;

    // ---------------------------------------------------------------
    // GET ALL
    // ---------------------------------------------------------------
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER','ROLE_STAFF')")
    public ResponseEntity<ApiResponse<Page<AdminProductResponse>>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(ApiResponse.<Page<AdminProductResponse>>builder()
                .data(adminProductService.getAllProducts(keyword, categoryId, isActive, page, size))
                .build());
    }

    // ---------------------------------------------------------------
    // GET ONE
    // ---------------------------------------------------------------
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER','ROLE_STAFF')")
    public ResponseEntity<ApiResponse<AdminProductResponse>> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<AdminProductResponse>builder()
                .data(adminProductService.getProductById(id)).build());
    }

    // ---------------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------------
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<AdminProductResponse>> create(
            @Valid @RequestBody AdminProductRequest request) {

        AdminProductResponse created = adminProductService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<AdminProductResponse>builder()
                        .status(201)
                        .message("Tạo sản phẩm thành công")
                        .data(created).build());
    }

    // ---------------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------------
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<AdminProductResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody AdminProductRequest request) {

        return ResponseEntity.ok(ApiResponse.<AdminProductResponse>builder()
                .message("Cập nhật sản phẩm thành công")
                .data(adminProductService.updateProduct(id, request)).build());
    }

    // ---------------------------------------------------------------
    // SOFT DELETE / RESTORE
    // ---------------------------------------------------------------
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        adminProductService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Đã ẩn sản phẩm thành công").build());
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> restore(@PathVariable Long id) {
        adminProductService.restoreProduct(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Đã khôi phục sản phẩm thành công").build());
    }

    // ---------------------------------------------------------------
    // EXPORT
    // ---------------------------------------------------------------
    @GetMapping("/export")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER','ROLE_ACCOUNTANT')")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "excel") String format) {
        byte[] data = adminProductService.exportProducts(format);
        String filename = "san-pham." + ("csv".equalsIgnoreCase(format) ? "csv" : "xlsx");
        String ct = "csv".equalsIgnoreCase(format)
                ? "text/csv; charset=UTF-8"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(ct))
                .body(data);
    }

    // ---------------------------------------------------------------
    // IMPORT
    // ---------------------------------------------------------------
    /**
     * POST /api/v1/admin/products/import
     * Content-Type: multipart/form-data
     * Form field: file (.xlsx | .xls | .csv)
     *
     * Template Excel/CSV cần có header:
     *   SKU | Tên sản phẩm | Danh mục | Giá gốc | Mô tả | Ảnh thumbnail
     */
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<IAdminProductService.ImportResult>> importProducts(
            @RequestParam("file") MultipartFile file) {

        IAdminProductService.ImportResult result = adminProductService.importProducts(file);
        String msg = String.format("Import hoàn tất: %d thành công, %d thất bại",
                result.success(), result.failed());
        return ResponseEntity.ok(ApiResponse.<IAdminProductService.ImportResult>builder()
                .message(msg).data(result).build());
    }
}
