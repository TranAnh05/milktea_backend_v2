package com.example.milktea_backend.controllers;

import com.example.milktea_backend.dtos.requests.AdminUserRequest;
import com.example.milktea_backend.dtos.responses.AdminUserResponse;
import com.example.milktea_backend.dtos.responses.ApiResponse;
import com.example.milktea_backend.services.interfaces.IAdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final IAdminUserService adminUserService;

    // ---------------------------------------------------------------
    // GET ALL — ADMIN, HR
    // ---------------------------------------------------------------
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_HR')")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(ApiResponse.<Page<AdminUserResponse>>builder()
                .data(adminUserService.getAllUsers(keyword, isActive, page, size))
                .build());
    }

    // ---------------------------------------------------------------
    // GET ONE
    // ---------------------------------------------------------------
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_HR')")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<AdminUserResponse>builder()
                .data(adminUserService.getUserById(id))
                .build());
    }

    // ---------------------------------------------------------------
    // CREATE STAFF — Chỉ ADMIN
    // ---------------------------------------------------------------
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserResponse>> createStaff(
            @Valid @RequestBody AdminUserRequest request) {

        AdminUserResponse created = adminUserService.createStaff(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<AdminUserResponse>builder()
                        .status(201)
                        .message("Tạo tài khoản nhân viên thành công")
                        .data(created)
                        .build());
    }

    // ---------------------------------------------------------------
    // UPDATE — ADMIN, HR
    // ---------------------------------------------------------------
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_HR')")
    public ResponseEntity<ApiResponse<AdminUserResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody AdminUserRequest request) {

        return ResponseEntity.ok(ApiResponse.<AdminUserResponse>builder()
                .message("Cập nhật tài khoản thành công")
                .data(adminUserService.updateUser(id, request))
                .build());
    }

    // ---------------------------------------------------------------
    // TOGGLE STATUS (LOCK / UNLOCK) — ADMIN, HR
    // ---------------------------------------------------------------
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_HR')")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(@PathVariable Long id) {
        adminUserService.toggleUserStatus(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Đã đổi trạng thái tài khoản")
                .build());
    }

    // ---------------------------------------------------------------
    // ASSIGN ROLES — Chỉ ADMIN
    // ---------------------------------------------------------------
    @PatchMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> assignRoles(
            @PathVariable Long id,
            @RequestBody Map<String, List<String>> body) {

        List<String> roleCodes = body.get("roleCodes");
        if (roleCodes == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>builder()
                            .status(400)
                            .message("Thiếu trường 'roleCodes'")
                            .build());
        }
        adminUserService.assignRoles(id, roleCodes);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Cập nhật quyền thành công")
                .build());
    }
}
