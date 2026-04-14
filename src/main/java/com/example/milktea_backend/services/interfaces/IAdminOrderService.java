package com.example.milktea_backend.services.interfaces;

import com.example.milktea_backend.dtos.requests.AdminOrderStatusRequest;
import com.example.milktea_backend.dtos.responses.AdminOrderResponse;
import com.example.milktea_backend.dtos.responses.OrderDetailResponse;
import com.example.milktea_backend.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

public interface IAdminOrderService {

    // Lấy tất cả đơn hàng (có filter + phân trang)
    Page<AdminOrderResponse> getAllOrders(
            String keyword, OrderStatus status,
            LocalDateTime from, LocalDateTime to,
            int page, int size, String sortBy, String sortDir);

    // Xem chi tiết đơn (dùng lại OrderDetailResponse của client)
    OrderDetailResponse getOrderDetail(String orderId);

    // Cập nhật trạng thái đơn
    void updateOrderStatus(String orderId, AdminOrderStatusRequest request);

    // Export đơn hàng ra byte[] (Excel hoặc CSV)
    byte[] exportOrders(LocalDateTime from, LocalDateTime to, OrderStatus status, String format);

    // Lấy danh sách đơn để export (dùng nội bộ)
    List<AdminOrderResponse> getOrdersForExport(LocalDateTime from, LocalDateTime to, OrderStatus status);
}
