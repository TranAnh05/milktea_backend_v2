package com.example.milktea_backend.services.implement;

import com.example.milktea_backend.dtos.requests.LoginRequest;
import com.example.milktea_backend.dtos.requests.RegisterRequest;
import com.example.milktea_backend.dtos.responses.AuthResponse;
import com.example.milktea_backend.entities.Role;
import com.example.milktea_backend.entities.User;
import com.example.milktea_backend.entities.VerificationToken;
import com.example.milktea_backend.repositories.RoleRepository;
import com.example.milktea_backend.repositories.UserRepository;
import com.example.milktea_backend.repositories.VerificationTokenRepository;
import com.example.milktea_backend.security.CustomUserDetails;
import com.example.milktea_backend.services.interfaces.IAuthService;
import com.example.milktea_backend.services.interfaces.IEmailService;
import com.example.milktea_backend.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final IEmailService emailService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

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

        if(!user.getIsVerified()) {
            throw new IllegalStateException("Tài khoản chưa được xác thực. Vui lòng kiểm tra email của bạn để kích hoạt!");
        }

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
                .phone(user.getPhone())
                .roles(roles)
                .build();

        // 6. Trả về Response cuối cùng
        return AuthResponse.builder()
                .token(jwt)
                .user(userDto)
                .build();
    }

    @Override
    public void register(RegisterRequest request) {
        // 1. Kiểm tra email trùng lặp
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email này đã được đăng ký trong hệ thống!");
        }

        // 2. Gán quyền mặc định là CUSTOMER
        Role customerRole = roleRepository.findByCode("ROLE_CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Lỗi hệ thống: Không tìm thấy quyền Khách hàng"));

        // 3. Tạo và lưu User (Trạng thái chưa xác thực)
        User newUser = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .isActive(true)
                .isVerified(false) // CHÚ Ý: Đặt là false
                .roles(Set.of(customerRole))
                .build();
        userRepository.save(newUser);

        // 4. Tạo Token ngẫu nhiên (UUID)
        String tokenString = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .token(tokenString)
                .user(newUser)
                .expiresAt(LocalDateTime.now().plusMinutes(15)) // Hết hạn sau 15 phút
                .build();
        tokenRepository.save(verificationToken);

        String verifyLink = frontendUrl + "/verify-email?token=" + tokenString;

        // 5. Gửi Email (Chạy ngầm)
        emailService.sendVerificationEmail(newUser.getEmail(), newUser.getFullName(), verifyLink);
    }

    @Override
    public void verifyEmail(String token) {
        // 1. Tìm token trong DB
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Link xác thực không hợp lệ hoặc không tồn tại!"));

        // 2. Kiểm tra hạn sử dụng
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(verificationToken); // Xóa token rác
            throw new IllegalArgumentException("Link xác thực đã hết hạn! Vui lòng yêu cầu gửi lại.");
        }

        // 3. Kích hoạt tài khoản
        User user = verificationToken.getUser();
        user.setIsVerified(true);
        userRepository.save(user);

        // 4. Xóa token sau khi dùng xong
        tokenRepository.delete(verificationToken);
    }
}
