package com.example.milktea_backend.controllers;

import com.example.milktea_backend.dtos.requests.LoginRequest;
import com.example.milktea_backend.dtos.requests.RegisterRequest;
import com.example.milktea_backend.dtos.responses.ApiResponse;
import com.example.milktea_backend.dtos.responses.AuthResponse;
import com.example.milktea_backend.services.interfaces.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @PostMapping("/login")
    // Chú ý: Bắt buộc phải có @Valid để kích hoạt kiểm tra @NotBlank, @Email trong DTO
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {

        AuthResponse authResponse = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.<AuthResponse>builder()
                        .message("Đăng nhập thành công")
                        .data(authResponse)
                        .build()
        );
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .message("Đăng ký thành công! Vui lòng kiểm tra email để xác thực.")
                        .build()
        );
    }

    @GetMapping("/verify")
    public ResponseEntity<Void> verifyEmail(@RequestParam("token") String token) {
        try {
            authService.verifyEmail(token);
            // Nếu thành công, chuyển hướng về trang Đăng nhập của Client kèm thông báo
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/login?verified=true"))
                    .build();
        } catch (Exception e) {
            // Nếu lỗi/hết hạn, chuyển hướng về trang lỗi
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/login?error=" + e.getMessage()))
                    .build();
        }
    }
}
