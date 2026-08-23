package com.example.milktea_backend.controllers;

import com.example.milktea_backend.dtos.requests.LoginRequest;
import com.example.milktea_backend.dtos.requests.RegisterRequest;
import com.example.milktea_backend.dtos.responses.ApiResponse;
import com.example.milktea_backend.dtos.responses.AuthResponse;
import com.example.milktea_backend.services.interfaces.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

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

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam("token") String token) {
        authService.verifyEmail(token);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Xác thực tài khoản thành công")
                .build());
    }
}
