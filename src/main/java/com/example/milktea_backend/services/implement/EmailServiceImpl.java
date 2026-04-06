package com.example.milktea_backend.services.implement;

import com.example.milktea_backend.services.interfaces.IEmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements IEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // Annotation @Async giúp việc gửi email chạy ngầm, không làm Frontend phải chờ lâu
    @Async
    @Override
    public void sendVerificationEmail(String toEmail, String fullName, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Xác thực tài khoản Trà Sữa");

            // Build nội dung Email bằng HTML cho đẹp mắt
            // Lưu ý: Đường dẫn này sẽ trỏ về Controller của Backend để xử lý logic trước
            String verificationLink = "http://localhost:8080/api/v1/auth/verify?token=" + token;

            String htmlContent = "<div style='font-family: Arial, sans-serif; padding: 20px;'>"
                    + "<h2>Chào " + fullName + ",</h2>"
                    + "<p>Cảm ơn bạn đã đăng ký tài khoản. Vui lòng click vào nút bên dưới để xác thực email của bạn (Link có hiệu lực trong 15 phút):</p>"
                    + "<a href='" + verificationLink + "' style='display: inline-block; padding: 10px 20px; color: white; background-color: #28a745; text-decoration: none; border-radius: 5px;'>Xác thực ngay</a>"
                    + "<p>Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.</p>"
                    + "</div>";

            helper.setText(htmlContent, true); // true để bật chế độ HTML
            mailSender.send(message);

        } catch (MessagingException e) {
            System.err.println("Lỗi khi gửi email: " + e.getMessage());
        }
    }
}
