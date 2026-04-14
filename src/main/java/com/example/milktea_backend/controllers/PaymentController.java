package com.example.milktea_backend.controllers;

import com.example.milktea_backend.dtos.requests.WebhookRequest;
import com.example.milktea_backend.services.interfaces.IPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final IPaymentService paymentService;
    @Value("${sepay.webhook.api-key}")
    private String sepayApiKey;

    // Phải là POST
    @PostMapping("/webhook")
    public ResponseEntity<String> receiveWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody WebhookRequest request) {
        try {
            log.info("🔑 Header Authorization nhận được từ SePay: '{}'", authorizationHeader);
            log.info("🔑 API Key gốc trong máy: '{}'", sepayApiKey);

            // Lam sach chuoi truoc khi so sanh
            String tokenToVerify = authorizationHeader;
            if(tokenToVerify != null) {
                tokenToVerify = tokenToVerify.replaceAll("(?i)Bearer ", "")
                        .replaceAll("(?i)ApiKey ", "")
                        .trim();
            }

            // 3. KIỂM TRA BẢO MẬT
            if (tokenToVerify == null || !tokenToVerify.equals(sepayApiKey.trim())) {
                log.error("Cảnh báo bảo mật: Sai API Key từ Webhook! Dấu hiệu giả mạo.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"success\": false, \"message\": \"Invalid API Key\"}");
            }

            paymentService.processWebhook(request);

            // Tra ve 200 ok cho sepay
            return ResponseEntity.ok("{\"success\": true}");
        } catch (Exception e) {
            log.error("Lỗi hệ thống khi xử lý Webhook: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"success\": false}");
        }
    }
}
