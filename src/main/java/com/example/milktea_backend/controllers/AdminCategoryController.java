// ============================================================
//  AdminCategoryController.java
// ============================================================
package com.example.milktea_backend.controllers;

import com.example.milktea_backend.dtos.requests.AdminCategoryRequest;
import com.example.milktea_backend.dtos.responses.ApiResponse;
import com.example.milktea_backend.dtos.responses.CategoryResponse;
import com.example.milktea_backend.services.interfaces.IAdminCategoryService;
import com.example.milktea_backend.services.interfaces.IAdminProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final IAdminCategoryService adminCategoryService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER','ROLE_STAFF')")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll(
            @RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(ApiResponse.<List<CategoryResponse>>builder()
                .data(adminCategoryService.getAllCategories(isActive)).build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<CategoryResponse>> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<CategoryResponse>builder()
                .data(adminCategoryService.getCategoryById(id)).build());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody AdminCategoryRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.<CategoryResponse>builder()
                .status(201).message("Tạo danh mục thành công")
                .data(adminCategoryService.createCategory(request)).build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable Long id, @Valid @RequestBody AdminCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.<CategoryResponse>builder()
                .message("Cập nhật danh mục thành công")
                .data(adminCategoryService.updateCategory(id, request)).build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        adminCategoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder().message("Đã ẩn danh mục").build());
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "excel") String format) {
        byte[] data = adminCategoryService.exportCategories(format);
        String filename = "danh-muc." + ("csv".equalsIgnoreCase(format) ? "csv" : "xlsx");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType("csv".equalsIgnoreCase(format)
                        ? MediaType.parseMediaType("text/csv; charset=UTF-8")
                        : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    /** Template CSV: Tên danh mục | Mô tả | Ảnh */
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<IAdminProductService.ImportResult>> importData(
            @RequestParam("file") MultipartFile file) {
        IAdminProductService.ImportResult result = adminCategoryService.importCategories(file);
        return ResponseEntity.ok(ApiResponse.<IAdminProductService.ImportResult>builder()
                .message(String.format("Import: %d thành công, %d thất bại", result.success(), result.failed()))
                .data(result).build());
    }
}
