package com.example.milktea_backend.services.implement;

import com.example.milktea_backend.dtos.requests.AdminOrderStatusRequest;
import com.example.milktea_backend.dtos.responses.AdminOrderResponse;
import com.example.milktea_backend.dtos.responses.OrderDetailResponse;
import com.example.milktea_backend.entities.Order;
import com.example.milktea_backend.entities.OrderItem;
import com.example.milktea_backend.entities.OrderItemTopping;
import com.example.milktea_backend.enums.OrderStatus;
import com.example.milktea_backend.exceptions.ResourceNotFoundException;
import com.example.milktea_backend.repositories.OrderRepository;
import com.example.milktea_backend.services.interfaces.IAdminOrderService;
import com.example.milktea_backend.utils.ExcelCsvHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements IAdminOrderService {

    private final OrderRepository orderRepository;
    private final ExcelCsvHelper excelCsvHelper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // =====================================================================
    //  DANH SÁCH ĐƠN HÀNG
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<AdminOrderResponse> getAllOrders(
            String keyword, OrderStatus status,
            LocalDateTime from, LocalDateTime to,
            int page, int size, String sortBy, String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        String kw = (keyword != null && !keyword.isBlank()) ? keyword : null;

        Page<Order> orders = orderRepository.findAllForAdmin(status, from, to, kw, pageable);
        return orders.map(this::mapToAdminOrderResponse);
    }

    // =====================================================================
    //  CHI TIẾT ĐƠN HÀNG (DÙNG LẠI mapToOrderDetailResponse của client)
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));
        return mapToOrderDetailResponse(order);
    }

    // =====================================================================
    //  CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG
    // =====================================================================

    @Override
    @Transactional
    public void updateOrderStatus(String orderId, AdminOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));

        OrderStatus newStatus = OrderStatus.valueOf(request.getOrderStatus());

        // Kiểm tra logic chuyển trạng thái hợp lệ
        validateStatusTransition(order.getOrderStatus(), newStatus);

        order.setOrderStatus(newStatus);
        if (newStatus == OrderStatus.CANCELLED && request.getCancelReason() != null) {
            order.setCancelReason(request.getCancelReason());
        }
        orderRepository.save(order);
    }

    // =====================================================================
    //  EXPORT
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public byte[] exportOrders(LocalDateTime from, LocalDateTime to, OrderStatus status, String format) {
        List<AdminOrderResponse> orders = getOrdersForExport(from, to, status);

        List<String> headers = List.of(
                "Mã đơn", "Khách hàng", "Số điện thoại", "Địa chỉ",
                "Tổng tiền hàng", "Phí ship", "Giảm giá", "Tổng thanh toán",
                "Phương thức TT", "Trạng thái TT", "Trạng thái đơn",
                "Voucher", "Ghi chú", "Ngày đặt"
        );

        List<List<Object>> rows = new ArrayList<>();
        for (AdminOrderResponse o : orders) {
            rows.add(List.of(
                    o.getOrderId(),
                    o.getGuestName(),
                    o.getGuestPhone(),
                    o.getGuestAddress(),
                    o.getSubTotal(),
                    o.getShippingFee(),
                    o.getDiscountAmount(),
                    o.getFinalTotal(),
                    o.getPaymentMethod().name(),
                    o.getPaymentStatus().name(),
                    o.getOrderStatus().name(),
                    o.getVoucherCode() != null ? o.getVoucherCode() : "",
                    o.getNote() != null ? o.getNote() : "",
                    o.getCreatedAt() != null ? o.getCreatedAt().format(DATE_FMT) : ""
            ));
        }

        try {
            if ("csv".equalsIgnoreCase(format)) {
                return excelCsvHelper.exportToCsv(headers, rows);
            } else {
                return excelCsvHelper.exportToExcel("Đơn hàng", headers, rows);
            }
        } catch (IOException e) {
            throw new RuntimeException("Lỗi xuất file: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminOrderResponse> getOrdersForExport(LocalDateTime from, LocalDateTime to, OrderStatus status) {
        List<Order> orders = orderRepository.findAllForExport(from, to, status);
        return orders.stream().map(this::mapToAdminOrderResponse).toList();
    }

    // =====================================================================
    //  PRIVATE HELPERS
    // =====================================================================

    private AdminOrderResponse mapToAdminOrderResponse(Order order) {
        int totalItems = order.getOrderItems().stream()
                .mapToInt(OrderItem::getQuantity).sum();

        return AdminOrderResponse.builder()
                .orderId(order.getId())
                .guestName(order.getGuestName())
                .guestPhone(order.getGuestPhone())
                .guestAddress(order.getGuestAddress())
                .note(order.getNote())
                .subTotal(order.getSubTotal())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .finalTotal(order.getFinalTotal())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .orderStatus(order.getOrderStatus())
                .cancelReason(order.getCancelReason())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .voucherCode(order.getVoucher() != null ? order.getVoucher().getCode() : null)
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .userEmail(order.getUser() != null ? order.getUser().getEmail() : null)
                .totalItemCount(totalItems)
                .build();
    }

    /** Tái sử dụng logic map chi tiết đơn (giống OrderServiceImpl của client) */
    private OrderDetailResponse mapToOrderDetailResponse(Order order) {
        List<OrderDetailResponse.OrderItemDto> itemDtos = order.getOrderItems().stream().map(item -> {
            List<OrderDetailResponse.OrderItemToppingDto> toppingDtos = item.getOrderItemToppings().stream().map(t ->
                    OrderDetailResponse.OrderItemToppingDto.builder()
                            .id(t.getId())
                            .toppingName(t.getToppingName())
                            .toppingPrice(t.getToppingPrice())
                            .build()
            ).toList();

            return OrderDetailResponse.OrderItemDto.builder()
                    .id(item.getId())
                    .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                    .productName(item.getProductName())
                    .productImage(item.getProductImage())
                    .sizeName(item.getSizeName())
                    .sugarLevel(item.getSugarLevel())
                    .iceLevel(item.getIceLevel())
                    .unitPrice(item.getUnitPrice())
                    .quantity(item.getQuantity())
                    .totalPrice(item.getTotalPrice())
                    .toppings(toppingDtos)
                    .build();
        }).toList();

        return OrderDetailResponse.builder()
                .orderId(order.getId())
                .createdAt(order.getCreatedAt())
                .orderStatus(order.getOrderStatus())
                .cancelReason(order.getCancelReason())
                .guestName(order.getGuestName())
                .guestPhone(order.getGuestPhone())
                .guestAddress(order.getGuestAddress())
                .note(order.getNote())
                .subTotal(order.getSubTotal())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .finalTotal(order.getFinalTotal())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .items(itemDtos)
                .build();
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        // Không cho phép quay ngược trạng thái đã hoàn thành / đã hủy
        if (current == OrderStatus.COMPLETED || current == OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Không thể chuyển trạng thái từ " + current + " sang " + next);
        }
        // Luồng hợp lệ: PENDING → CONFIRMED → PREPARING → DELIVERING → COMPLETED | CANCELLED
        boolean valid = switch (current) {
            case PENDING    -> next == OrderStatus.CONFIRMED  || next == OrderStatus.CANCELLED;
            case CONFIRMED  -> next == OrderStatus.PREPARING  || next == OrderStatus.CANCELLED;
            case PREPARING  -> next == OrderStatus.DELIVERING || next == OrderStatus.CANCELLED;
            case DELIVERING -> next == OrderStatus.COMPLETED  || next == OrderStatus.CANCELLED;
            default         -> false;
        };
        if (!valid) {
            throw new IllegalStateException(
                    "Chuyển trạng thái không hợp lệ: " + current + " → " + next);
        }
    }
}
