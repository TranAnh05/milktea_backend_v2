package com.example.milktea_backend.services.implement;

import com.example.milktea_backend.dtos.requests.CartItemRequest;
import com.example.milktea_backend.dtos.requests.OrderRequest;
import com.example.milktea_backend.dtos.responses.OrderDetailResponse;
import com.example.milktea_backend.dtos.responses.OrderHistoryResponse;
import com.example.milktea_backend.dtos.responses.PlaceOrderResponse;
import com.example.milktea_backend.entities.*;
import com.example.milktea_backend.enums.DiscountType;
import com.example.milktea_backend.enums.OrderStatus;
import com.example.milktea_backend.enums.PaymentMethod;
import com.example.milktea_backend.repositories.*;
import com.example.milktea_backend.services.interfaces.ICartService;
import com.example.milktea_backend.services.interfaces.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final SizeRepository sizeRepository;
    private final ToppingRepository toppingRepository;
    private final VoucherRepository voucherRepository;
    private final ProductSizeRepository productSizeRepository;
    private final ProductPromotionRepository promotionRepository;
    private final ICartService cartService;
    private final VoucherUsageRepository voucherUsageRepository;

    @Override
    @Transactional
    public PlaceOrderResponse placeOrder(Long userId, OrderRequest request) {
        // 1. KHỞI TẠO ĐƠN HÀNG MỚI
        Order order = new Order();
        // Tạo mã đơn hàng độc nhất: ORD- + Timestamp (Hoặc 8 ký tự random)
        String orderId = "ORD-" + System.currentTimeMillis();
        order.setId(orderId);

        // Nạp thông tin khách hàng & vận chuyển
        if (userId != null) {
            User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));
            order.setUser(user);
        }
        order.setGuestName(request.getCustomerName());
        order.setGuestPhone(request.getPhone());
        order.setGuestAddress(request.getAddress());
        order.setNote(request.getNote());
        order.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentMethod()));
        // Mặc định tạo đơn sẽ là PENDING và UNPAID

        int subTotal = 0;
        List<OrderItem> orderItems = new ArrayList<>();
        VoucherUsage pendingVoucherUsage = null;

        // 2. XỬ LÝ DANH SÁCH ITEM (PHÂN LUỒNG USER / GUEST)
        if (userId != null) {
            // ---> LUỒNG 1: USER ĐÃ ĐĂNG NHẬP (Lấy trực tiếp từ Database)
            Cart cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giỏ hàng"));
            List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());

            if (cartItems.isEmpty()) throw new IllegalArgumentException("Giỏ hàng đang trống");

            for (CartItem ci : cartItems) {
                int itemTotalPrice = ci.getPrice() * ci.getQuantity();
                subTotal += itemTotalPrice;

                // Chụp Snapshot (bản sao) dữ liệu lúc mua
                OrderItem oi = OrderItem.builder()
                        .order(order)
                        .product(ci.getProduct())
                        .productName(ci.getProduct().getName())
                        .productImage(ci.getProduct().getThumbnailUrl())
                        .sizeName(ci.getSize().getName())
                        .sugarLevel(ci.getSugarLevel())
                        .iceLevel(ci.getIceLevel())
                        .unitPrice(ci.getPrice())
                        .quantity(ci.getQuantity())
                        .totalPrice(itemTotalPrice)
                        .build();

                // Lấy Topping đi kèm
                for (CartItemTopping cit : ci.getToppings()) {
                    OrderItemTopping oit = OrderItemTopping.builder()
                            .orderItem(oi)
                            .toppingName(cit.getTopping().getName())
                            .toppingPrice(cit.getPrice())
                            .build();
                    oi.getOrderItemToppings().add(oit);
                }
                orderItems.add(oi);
            }
        } else {
            // ---> LUỒNG 2: KHÁCH VÃNG LAI GUEST (Tính toán lại giá bảo mật từ request)
            if (request.getGuestItems() == null || request.getGuestItems().isEmpty()) {
                throw new IllegalArgumentException("Giỏ hàng đang trống");
            }

            for (CartItemRequest reqItem : request.getGuestItems()) {
                // BƯỚC BẢO MẬT: Gọi hàm phụ để tính lại giá từ DB (Không dùng giá của Frontend)
                int unitPrice = calculateSecureUnitPrice(reqItem);
                int itemTotalPrice = unitPrice * reqItem.getQuantity();
                subTotal += itemTotalPrice;

                Product product = productRepository.findById(reqItem.getProductId()).orElseThrow();
                Size size = sizeRepository.findById(reqItem.getSizeId()).orElseThrow();

                OrderItem oi = OrderItem.builder()
                        .order(order)
                        .product(product)
                        .productName(product.getName())
                        .productImage(product.getThumbnailUrl())
                        .sizeName(size.getName())
                        .sugarLevel(reqItem.getSugarLevel())
                        .iceLevel(reqItem.getIceLevel())
                        .unitPrice(unitPrice)
                        .quantity(reqItem.getQuantity())
                        .totalPrice(itemTotalPrice)
                        .build();

                if (reqItem.getToppingIds() != null) {
                    for (Long tId : reqItem.getToppingIds()) {
                        Topping topping = toppingRepository.findById(tId).orElseThrow();
                        OrderItemTopping oit = OrderItemTopping.builder()
                                .orderItem(oi)
                                .toppingName(topping.getName())
                                .toppingPrice(topping.getPrice())
                                .build();
                        oi.getOrderItemToppings().add(oit);
                    }
                }
                orderItems.add(oi);
            }
        }

        // 3. TÍNH TOÁN TỔNG TIỀN CUỐI CÙNG & VOUCHER
        int shippingFee = 0;
        int discountAmount = 0;

        if (request.getVoucherId() != null) {
            Voucher voucher = voucherRepository.findById(request.getVoucherId())
                    .orElseThrow(() -> new IllegalArgumentException("Mã giảm giá không tồn tại"));

            // --- KIỂM TRA LẠI 4 LỚP BẢO MẬT TRƯỚC KHI TẠO ĐƠN ---
            if (!voucher.getIsActive()) {
                throw new IllegalArgumentException("Mã giảm giá đã bị khóa.");
            }
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(voucher.getStartDate()) || now.isAfter(voucher.getEndDate())) {
                throw new IllegalArgumentException("Mã giảm giá đã hết hạn.");
            }
            if (voucher.getQuantity() <= 0) {
                throw new IllegalStateException("Rất tiếc, mã giảm giá này vừa hết lượt sử dụng.");
            }
            if (subTotal < voucher.getMinOrderAmount()) {
                throw new IllegalArgumentException("Đơn hàng chưa đạt mức tối thiểu " + voucher.getMinOrderAmount() + "đ để áp dụng mã này.");
            }

            // --- NẾU LỌT QUA HẾT THÌ BẮT ĐẦU TÍNH TOÁN ---
            if (voucher.getDiscountType() == DiscountType.FIXED_AMOUNT) {
                discountAmount = voucher.getDiscountValue();
            } else {
                discountAmount = (subTotal * voucher.getDiscountValue()) / 100;
                if (voucher.getMaxDiscountAmount() != null && discountAmount > voucher.getMaxDiscountAmount()) {
                    discountAmount = voucher.getMaxDiscountAmount(); // Cắt trần
                }
            }
            order.setVoucher(voucher);

            // Trừ tồn kho và Lưu lịch sử Usage
            voucher.setQuantity(voucher.getQuantity() - 1);
            voucherRepository.save(voucher);

            pendingVoucherUsage = VoucherUsage.builder()
                    .user(order.getUser())
                    .voucher(voucher)
                    .order(order) // Sửa lại thành lưu String orderId cho đúng thiết kế entity
                    .discountAmount(discountAmount)
                    .build();

        }

        int finalTotal = subTotal + shippingFee - discountAmount;
        if (finalTotal < 0) finalTotal = 0; // Tránh lỗi âm tiền

        // 4. LƯU VÀO DATABASE
        order.setOrderItems(orderItems); // Mảng OrderItems đã chứa cả OrderItemToppings
        order.setSubTotal(subTotal);
        order.setShippingFee(shippingFee);
        order.setDiscountAmount(discountAmount);
        order.setFinalTotal(finalTotal);

        orderRepository.save(order); // CascadeType.ALL sẽ tự động lưu mảng orderItems và toppings con

        if (pendingVoucherUsage != null) {
            voucherUsageRepository.save(pendingVoucherUsage);
        }

        // 5. DỌN DẸP GIỎ HÀNG (Chỉ áp dụng nếu là User)
        if (userId != null) {
            cartService.clearCart(userId);
        }

        // Trả về mã đơn hàng cho Frontend (để Frontend chuyển sang trang Thank You)
        return PlaceOrderResponse.builder()
                .orderId(orderId)
                .finalTotal(finalTotal)
                .paymentMethod(request.getPaymentMethod())
                .build();
    }

    private int calculateSecureUnitPrice(CartItemRequest request) {
        Product product = productRepository.findById(request.getProductId()).orElseThrow();
        int finalPrice = product.getBasePrice();

        // Khuyến mãi
        Optional<ProductPromotion> activePromo = promotionRepository.findActivePromotionByProductId(product.getId(), LocalDateTime.now());
        if (activePromo.isPresent()) {
            ProductPromotion promo = activePromo.get();
            if (promo.getDiscountType() == DiscountType.PERCENT) finalPrice = finalPrice - (finalPrice * promo.getDiscountValue() / 100);
            else finalPrice = Math.max(0, finalPrice - promo.getDiscountValue());
        }
        // Size
        ProductSize ps = productSizeRepository.findByProductIdAndSizeId(request.getProductId(), request.getSizeId()).orElseThrow();
        finalPrice += ps.getPriceSurcharge();

        // Topping
        if (request.getToppingIds() != null) {
            for (Long tid : request.getToppingIds()) {
                Topping t = toppingRepository.findById(tid).orElseThrow();
                finalPrice += t.getPrice();
            }
        }
        return finalPrice;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderHistoryResponse> getMyOrders(Long userId, OrderStatus status, int page, int size) {
        // Sắp xếp ngày tạo giảm dần (Mới nhất lên đầu)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Order> orders;
        if (status != null) {
            orders = orderRepository.findByUserIdAndOrderStatus(userId, status, pageable);
        } else {
            orders = orderRepository.findByUserId(userId, pageable);
        }

        // Chuyển đổi (Map) từ Entity sang DTO
        return orders.map(order -> {
            String firstItemName = "";
            String firstItemImage = "";
            int totalItems = 0;

            if (!order.getOrderItems().isEmpty()) {
                OrderItem firstItem = order.getOrderItems().get(0);
                firstItemName = firstItem.getProductName();
                firstItemImage = firstItem.getProductImage();
                // Tính tổng số lượng ly trà sữa trong đơn
                totalItems = order.getOrderItems().stream().mapToInt(OrderItem::getQuantity).sum();
            }

            return OrderHistoryResponse.builder()
                    .orderId(order.getId())
                    .finalTotal(order.getFinalTotal())
                    .orderStatus(order.getOrderStatus())
                    .createdAt(order.getCreatedAt())
                    .firstItemName(firstItemName)
                    .firstItemImage(firstItemImage)
                    .totalItemCount(totalItems)
                    .build();
        });
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long userId, String orderId) {
        // 1. Tìm đơn hàng & Chốt bảo mật: Đơn của ai người đó xem
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng hoặc bạn không có quyền truy cập"));

        // 2. Map dữ liệu món ăn và topping
        return mapToOrderDetailResponse(order);
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, String orderId, String cancelReason) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        // Kiểm tra xem đơn đã bị hủy trước đó chưa
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Đơn hàng này đã bị hủy rồi");
        }

        // Chỉ cho phép hủy khi đang ở trạng thái PENDING
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Không thể hủy đơn hàng đang được xử lý hoặc đã hoàn thành. Vui lòng gọi Hotline!");
        }

        // Thực hiện hủy
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelReason(cancelReason != null ? cancelReason : "Khách hàng tự hủy trên hệ thống");

        orderRepository.save(order);

        //  HOÀN TRẢ VOUCHER
        if (order.getVoucher() != null) {
            Voucher voucher = order.getVoucher();

            // 1. Cộng trả lại 1 lượt cho Voucher đó
            voucher.setQuantity(voucher.getQuantity() + 1);
            voucherRepository.save(voucher);

            // 2. Xóa lịch sử dùng Voucher của cái bill bị hủy này (Để lần sau user còn xài được mã đó nữa)
            voucherUsageRepository.deleteByOrderId(order.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse trackGuestOrder(String orderId, String phone) {
        Order order = orderRepository.findByIdAndGuestPhone(orderId, phone)
                .orElseThrow(() -> new IllegalArgumentException("Mã đơn hàng hoặc số điện thoại không chính xác"));
        return mapToOrderDetailResponse(order);
    }

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

    @Override
    @Transactional(readOnly = true)
    public String checkPaymentStatus(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
        return order.getPaymentStatus().name(); // Trả về "UNPAID" hoặc "PAID"
    }
}
