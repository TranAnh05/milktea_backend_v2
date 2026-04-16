package com.example.milktea_backend.services;

import com.example.milktea_backend.entities.Order;
import com.example.milktea_backend.entities.Voucher;
import com.example.milktea_backend.enums.OrderStatus;
import com.example.milktea_backend.enums.PaymentMethod;
import com.example.milktea_backend.enums.PaymentStatus;
import com.example.milktea_backend.repositories.OrderRepository;
import com.example.milktea_backend.repositories.VoucherRepository;
import com.example.milktea_backend.repositories.VoucherUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCleanupService {

    private final OrderRepository orderRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;

    // Chạy định kỳ mỗi 5 phút để quét dọn hệ thống
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void cleanupAbandonedOrders() {
        log.info("Bắt đầu tiến trình quét đơn hàng quá hạn thanh toán...");

        LocalDateTime deadline = LocalDateTime.now().minusMinutes(15);
        List<Order> expiredOrders = orderRepository.findExpiredOrders(
                PaymentStatus.UNPAID,
                PaymentMethod.BANK_TRANSFER,
                deadline
        );

        if (expiredOrders.isEmpty()) return;

        for (Order order : expiredOrders) {
            // 1. Hủy đơn
            order.setOrderStatus(OrderStatus.CANCELLED);
            order.setCancelReason("Hệ thống tự động hủy do quá hạn thời gian thanh toán (15 phút)");

            // 2. HOÀN TRẢ VOUCHER (Giống hệt logic em đã viết ở hàm khách tự hủy)
            if (order.getVoucher() != null) {
                Voucher voucher = order.getVoucher();

                // Cộng lại 1 lượt cho hệ thống
                voucher.setQuantity(voucher.getQuantity() + 1);
                voucherRepository.save(voucher);

                // Xóa lịch sử sử dụng để khách có thể dùng lại mã này cho đơn sau
                voucherUsageRepository.deleteByOrderId(order.getId());

                log.info("Đã hoàn trả Voucher {} cho đơn bị hủy {}", voucher.getCode(), order.getId());
            }

            log.info("Đã tự động hủy đơn hàng: {}", order.getId());
        }

        orderRepository.saveAll(expiredOrders);
    }
}
