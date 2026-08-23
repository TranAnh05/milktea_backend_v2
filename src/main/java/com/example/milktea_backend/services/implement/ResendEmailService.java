package com.example.milktea_backend.services.implement;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.example.milktea_backend.dtos.requests.ResendEmailRequest;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResendEmailService {

    private final RestClient resendRestClient;

    @Value("${resend.from-email}")
    private String defaultFromEmail;

    @Async 
    public void sendVerificationEmail(String toEmail, String fullName, String verifyLink) {
        String htmlContent = """
            <div style="font-family: Arial, sans-serif; padding: 20px;">
                <h2>Xin chào %s,</h2>
                <p>Cảm ơn bạn đã đăng ký tài khoản. Vui lòng nhấn vào liên kết bên dưới để xác thực email của bạn (liên kết có hiệu lực trong 15 phút):</p>
                <a href="%s" style="background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; display: inline-block; border-radius: 5px;">Xác thực tài khoản</a>
            </div>
            """.formatted(fullName, verifyLink);

        ResendEmailRequest payload = ResendEmailRequest.builder()
                .from(defaultFromEmail)
                .to(List.of(toEmail))
                .subject("Xác thực địa chỉ Email")
                .html(htmlContent)
                .build();

        resendRestClient.post()
                .uri("/emails")
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}
