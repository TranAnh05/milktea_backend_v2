package com.example.milktea_backend.services.interfaces;

import com.example.milktea_backend.dtos.requests.OrderRequest;
import com.example.milktea_backend.dtos.responses.OrderDetailResponse;
import com.example.milktea_backend.dtos.responses.OrderHistoryResponse;
import com.example.milktea_backend.dtos.responses.PlaceOrderResponse;
import com.example.milktea_backend.entities.Order;
import com.example.milktea_backend.enums.OrderStatus;
import org.springframework.data.domain.Page;

public interface IOrderService {
    // Trả về Mã đơn hàng (String) sau khi tạo thành công
    PlaceOrderResponse placeOrder(Long userId, OrderRequest request);

    Page<OrderHistoryResponse> getMyOrders(Long userId, OrderStatus status, int page, int size);

    OrderDetailResponse getOrderDetail(Long userId, String orderId);

    OrderDetailResponse trackGuestOrder(String orderId, String phone);

    void cancelOrder(Long userId, String orderId, String cancelReason);

    String checkPaymentStatus(String orderId);
}
