package com.example.milktea_backend.services.implement;

import com.example.milktea_backend.dtos.requests.AdminUserRequest;
import com.example.milktea_backend.dtos.responses.AdminUserResponse;
import com.example.milktea_backend.entities.Role;
import com.example.milktea_backend.entities.User;
import com.example.milktea_backend.exceptions.ResourceNotFoundException;
import com.example.milktea_backend.repositories.RoleRepository;
import com.example.milktea_backend.repositories.UserRepository;
import com.example.milktea_backend.services.interfaces.IAdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements IAdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getAllUsers(String keyword, Boolean isActive, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        String kw = (keyword != null && !keyword.isBlank()) ? keyword : null;
        return userRepository.findAllForAdmin(kw, isActive, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(Long id) {
        return mapToResponse(findUserOrThrow(id));
    }

    @Override
    @Transactional
    public AdminUserResponse createStaff(AdminUserRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email '" + request.getEmail() + "' đã tồn tại");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống khi tạo tài khoản mới");
        }

        Set<Role> roles = resolveRoles(request.getRoleCodes());

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .isVerified(true)   // Admin tạo thì coi như đã verified
                .isActive(request.getIsActive())
                .roles(roles)
                .build();

        return mapToResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public AdminUserResponse updateUser(Long id, AdminUserRequest request) {
        User user = findUserOrThrow(id);

        // Đổi email — kiểm tra trùng
        if (!user.getEmail().equals(request.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Email '" + request.getEmail() + "' đã được sử dụng");
            }
            user.setEmail(request.getEmail());
        }

        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setIsActive(request.getIsActive());

        // Đổi mật khẩu chỉ khi có truyền vào
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        // Cập nhật roles
        if (request.getRoleCodes() != null) {
            user.setRoles(resolveRoles(request.getRoleCodes()));
        }

        return mapToResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long id) {
        User user = findUserOrThrow(id);
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, List<String> roleCodes) {
        User user = findUserOrThrow(userId);
        user.setRoles(resolveRoles(roleCodes));
        userRepository.save(user);
    }

    // =====================================================================
    //  PRIVATE HELPERS
    // =====================================================================

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user ID: " + id));
    }

    private AdminUserResponse mapToResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getCode).collect(Collectors.toList());
        Long totalOrders = userRepository.countOrdersByUserId(user.getId());

        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .isVerified(user.getIsVerified())
                .isActive(user.getIsActive())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .totalOrders(totalOrders)
                .build();
    }

    private Set<Role> resolveRoles(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) return new HashSet<>();
        Set<Role> roles = new HashSet<>();
        for (String code : roleCodes) {
            Role role = roleRepository.findByCode(code)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy role: " + code));
            roles.add(role);
        }
        return roles;
    }
}
