package com.example.milktea_backend.services.implement;

import com.example.milktea_backend.dtos.requests.WebhookRequest;
import com.example.milktea_backend.entities.Order;
import com.example.milktea_backend.entities.PaymentTransaction;
import com.example.milktea_backend.enums.OrderStatus;
import com.example.milktea_backend.enums.PaymentStatus;
import com.example.milktea_backend.enums.TransactionStatus;
import com.example.milktea_backend.repositories.OrderRepository;
import com.example.milktea_backend.repositories.PaymentTransactionRepository;
import com.example.milktea_backend.services.interfaces.IPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j // Dùng để in log ra console
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements IPaymentService {

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Override
    @Transactional
     public void processWebhook(WebhookRequest request) {

        // 1. CHỐNG GỌI TRÙNG LẶP
        // Tránh việc SePay lag gửi 2 lần khiến 1 đơn bị lưu 2 transaction
        if (paymentTransactionRepository.existsByTransactionNo(request.getTransactionNo())) {
            log.info("Giao dịch {} đã được xử lý trước đó. Bỏ qua để tránh duplicate.", request.getTransactionNo());
            return;
        }

        // 2. Trích xuất mã đơn hàng từ Nội dung chuyển khoản
        String orderId = extractOrderId(request.getContent());
        if (orderId == null) {
            log.warn("Không tìm thấy mã đơn hàng trong nội dung: {}", request.getContent());
            return;
        }

        // 3. Tìm đơn hàng
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("Đơn hàng {} không tồn tại trong hệ thống!", orderId);
            return;
        }

        // 4. Bỏ qua nếu đơn hàng đã được thanh toán hoặc đã hủy
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            log.info("Đơn hàng {} đã được thanh toán trước đó.", orderId);
            return;
        }

        // 5. Kiểm tra số tiền
        TransactionStatus status = TransactionStatus.PENDING;

        if (request.getAmountIn() >= order.getFinalTotal()) {
            status = TransactionStatus.SUCCESS;
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setOrderStatus(OrderStatus.PREPARING);
        } else {
            status = TransactionStatus.FAILED;
            log.warn("Khách thanh toán thiếu tiền cho đơn {}! Cần: {}, Nhận: {}", orderId, order.getFinalTotal(), request.getAmountIn());
        }

        // 6. Ghi lại lịch sử vào bảng PaymentTransaction
        PaymentTransaction transaction = PaymentTransaction.builder()
                .order(order)
                .transactionNo(request.getTransactionNo())
                .amount(request.getAmountIn())
                .paymentGateway(request.getGateway() != null ? request.getGateway() : "VIETQR")
                .status(status)
                .rawLog(request.toString()) // Lưu log thô để đối soát
                .build();

        paymentTransactionRepository.save(transaction);
        orderRepository.save(order);

        log.info("Xử lý Webhook thành công cho đơn: {}. Trạng thái: {}", orderId, status);
    }

    // Hàm phụ: Dùng Regex để tìm chuỗi "ORD-xxx" trong một đoạn text bất kỳ
    private String extractOrderId(String content) {
        if (content == null) return null;
        // Quét tìm cụm từ bắt đầu bằng ORD- theo sau là các chữ số
        Pattern pattern = Pattern.compile("(ORD-?\\d+)");
        Matcher matcher = pattern.matcher(content.toUpperCase());
        if (matcher.find()) {
            String rawMatch = matcher.group(1);
            if(!rawMatch.contains("-")) {
                return rawMatch.replace("ORD", "ORD-");
            }
            return rawMatch;
        }
        return null;
    }
}
