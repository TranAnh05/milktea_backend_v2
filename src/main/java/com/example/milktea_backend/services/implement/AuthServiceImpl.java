package com.example.milktea_backend.services.implement;

import com.example.milktea_backend.dtos.requests.LoginRequest;
import com.example.milktea_backend.dtos.responses.AuthResponse;
import com.example.milktea_backend.entities.User;
import com.example.milktea_backend.security.CustomUserDetails;
import com.example.milktea_backend.services.interfaces.IAuthService;
import com.example.milktea_backend.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @Override
    public AuthResponse login(LoginRequest request) {
        // 1. Xác thực tài khoản bằng Spring Security
        // Nếu sai mật khẩu hoặc không tìm thấy user, nó sẽ tự động ném ra lỗi (Đã được GlobalExceptionHandler bắt)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // 2. Nếu code chạy xuống được đây nghĩa là xác thực thành công
        // Lấy thông tin user đã được bọc trong CustomUserDetails
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        // 3. Nhờ JwtUtils tạo chuỗi Token
        String jwt = jwtUtils.generateJwtToken(user.getEmail());

        // 4. Lấy danh sách quyền (Roles) để trả về cho Frontend
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // 5. Đóng gói dữ liệu người dùng
        AuthResponse.UserDto userDto = AuthResponse.UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roles)
                .build();

        // 6. Trả về Response cuối cùng
        return AuthResponse.builder()
                .token(jwt)
                .user(userDto)
                .build();
    }
}
