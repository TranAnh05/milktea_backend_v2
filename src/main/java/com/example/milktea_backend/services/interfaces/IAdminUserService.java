package com.example.milktea_backend.services.interfaces;

import com.example.milktea_backend.dtos.requests.AdminUserRequest;
import com.example.milktea_backend.dtos.responses.AdminUserResponse;
import org.springframework.data.domain.Page;

public interface IAdminUserService {

    Page<AdminUserResponse> getAllUsers(String keyword, Boolean isActive, int page, int size);

    AdminUserResponse getUserById(Long id);

    AdminUserResponse createStaff(AdminUserRequest request);  // Tạo tài khoản nhân viên

    AdminUserResponse updateUser(Long id, AdminUserRequest request);

    void toggleUserStatus(Long id);  // Khóa / Mở khóa tài khoản

    void assignRoles(Long userId, java.util.List<String> roleCodes);
}
