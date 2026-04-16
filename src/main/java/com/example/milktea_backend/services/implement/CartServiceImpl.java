package com.example.milktea_backend.services.implement;

import com.example.milktea_backend.dtos.requests.CartItemRequest;
import com.example.milktea_backend.dtos.requests.MergeCartRequest;
import com.example.milktea_backend.dtos.responses.CartItemResponse;
import com.example.milktea_backend.dtos.responses.CartResponse;
import com.example.milktea_backend.dtos.responses.CartToppingResponse;
import com.example.milktea_backend.entities.*;
import com.example.milktea_backend.enums.DiscountType;
import com.example.milktea_backend.repositories.*;
import com.example.milktea_backend.services.interfaces.ICartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements ICartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartItemToppingRepository cartItemToppingRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductSizeRepository productSizeRepository;
    private final ToppingRepository toppingRepository;
    private final ProductPromotionRepository promotionRepository;
    private final SizeRepository sizeRepository;

    @Override
    @Transactional // Bắt buộc có để đảm bảo nếu lỗi giữa chừng thì sẽ roll-back toàn bộ
    public void addOrUpdateCartItem(Long userId, CartItemRequest request) {
        // 1. Tìm giỏ hàng của User, nếu chưa có thì tạo mới
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy User"));
            Cart newCart = new Cart();
            newCart.setUser(user);
            return cartRepository.save(newCart);
        });

        // 2. Tìm xem món này (chính xác cấu hình này) đã có trong giỏ chưa
        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndItemSignature(cart.getId(), request.getSignature());

        if (existingItem.isPresent()) {
            // NẾU CÓ RỒI: Chỉ việc cộng dồn số lượng (Update)
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            // NẾU CHƯA CÓ: Tạo mới hoàn toàn (Insert)
            // BƯỚC BẢO MẬT: Tính lại giá tiền từ Database
            int unitPrice = calculateSecureUnitPrice(request);

            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            // Dùng getReferenceById để tối ưu hiệu năng (chỉ lấy proxy, không query DB thừa)
            newItem.setProduct(productRepository.getReferenceById(request.getProductId()));
            newItem.setSize(sizeRepository.getReferenceById(request.getSizeId()));
            newItem.setSugarLevel(request.getSugarLevel());
            newItem.setIceLevel(request.getIceLevel());
            newItem.setPrice(unitPrice);
            newItem.setQuantity(request.getQuantity());
            newItem.setItemSignature(request.getSignature());

            newItem = cartItemRepository.save(newItem);

            // 3. Lưu Topping đi kèm (Nếu khách có chọn)
            if (request.getToppingIds() != null && !request.getToppingIds().isEmpty()) {
                for (Long toppingId : request.getToppingIds()) {
                    Topping topping = toppingRepository.findById(toppingId)
                            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Topping"));

                    CartItemTopping cit = new CartItemTopping();
                    cit.setCartItem(newItem);
                    cit.setTopping(topping);
                    cit.setPrice(topping.getPrice()); // Chốt cứng giá Topping tại thời điểm mua

                    cartItemToppingRepository.save(cit);
                }
            }
        }
    }

    @Override
    @Transactional
    public void mergeCart(Long userId, MergeCartRequest request) {
        if (request.getCartItems() == null || request.getCartItems().isEmpty()) {
            return; // Nếu giỏ local rỗng thì không cần gộp
        }

        // Chạy vòng lặp, tận dụng lại chính hàm addOrUpdate ở trên!
        for (CartItemRequest itemReq : request.getCartItems()) {
            addOrUpdateCartItem(userId, itemReq);
        }
    }

    // --- HÀM PHỤ TRỢ: TÍNH GIÁ TIỀN AN TOÀN TỪ DATABASE ---
    private int calculateSecureUnitPrice(CartItemRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));

        // Giá gốc
        int finalPrice = product.getBasePrice();

        // Trừ đi khuyến mãi (nếu có)
        Optional<ProductPromotion> activePromo = promotionRepository.findActivePromotionByProductId(product.getId(), LocalDateTime.now());
        if (activePromo.isPresent()) {
            ProductPromotion promo = activePromo.get();
            if (promo.getDiscountType() == DiscountType.PERCENT) {
                finalPrice = finalPrice - (finalPrice * promo.getDiscountValue() / 100);
            } else {
                finalPrice = Math.max(0, finalPrice - promo.getDiscountValue());
            }
        }

        // Cộng phụ thu Size
        ProductSize ps = productSizeRepository.findByProductIdAndSizeId(request.getProductId(), request.getSizeId())
                .orElseThrow(() -> new IllegalArgumentException("Size không hợp lệ cho sản phẩm này"));
        finalPrice += ps.getPriceSurcharge();

        // Cộng tiền Topping
        if (request.getToppingIds() != null) {
            for (Long tid : request.getToppingIds()) {
                Topping t = toppingRepository.findById(tid).orElseThrow();
                finalPrice += t.getPrice();
            }
        }

        return finalPrice;
    }

    @Override
    @Transactional(readOnly = true) // Tối ưu hiệu năng vì hàm này chỉ đọc dữ liệu
    public CartResponse getCart(Long userId) {
        // 1. Tìm giỏ hàng. Nếu User chưa từng có giỏ hàng, trả về một giỏ rỗng
        Optional<Cart> optionalCart = cartRepository.findByUserId(userId);
        if (optionalCart.isEmpty()) {
            return CartResponse.builder()
                    .cartItems(List.of())
                    .cartCount(0)
                    .cartTotal(0)
                    .build();
        }

        Cart cart = optionalCart.get();
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());

        // 2. Chuyển đổi (Map) Entity sang DTO
        List<CartItemResponse> itemResponses = items.stream().map(item -> {

            // Lấy danh sách Topping của Item này
            List<CartToppingResponse> toppingResponses = cartItemToppingRepository.findByCartItemId(item.getId())
                    .stream().map(cit -> CartToppingResponse.builder()
                            .id(cit.getTopping().getId())
                            .name(cit.getTopping().getName())
                            .price(cit.getPrice()) // Lấy giá chốt lúc mua, không lấy giá hiện tại
                            .build())
                    .collect(Collectors.toList());

            int itemTotalPrice = item.getPrice() * item.getQuantity();

            return CartItemResponse.builder()
                    .signature(item.getItemSignature())
                    .productId(item.getProduct().getId())
                    .slug(item.getProduct().getSlug())
                    .productName(item.getProduct().getName())
                    .thumbnailUrl(item.getProduct().getThumbnailUrl())
                    .sizeId(item.getSize().getId())
                    .sizeName(item.getSize().getName())
                    .sugarLevel(item.getSugarLevel())
                    .iceLevel(item.getIceLevel())
                    .unitPrice(item.getPrice())
                    .quantity(item.getQuantity())
                    .totalPrice(itemTotalPrice)
                    .toppings(toppingResponses)
                    .build();
        }).collect(Collectors.toList());

        // 3. Tính toán tổng
        int cartCount = itemResponses.stream().mapToInt(CartItemResponse::getQuantity).sum();
        int cartTotal = itemResponses.stream().mapToInt(CartItemResponse::getTotalPrice).sum();

        return CartResponse.builder()
                .cartItems(itemResponses)
                .cartCount(cartCount)
                .cartTotal(cartTotal)
                .build();
    }

    @Override
    @Transactional
    public void updateCartItemQuantity(Long userId, String signature, Integer quantity) {
        if (quantity < 1) {
            removeCartItem(userId, signature);
            return;
        }

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Giỏ hàng không tồn tại"));

        CartItem item = cartItemRepository.findByCartIdAndItemSignature(cart.getId(), signature)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại trong giỏ"));

        item.setQuantity(quantity);
        cartItemRepository.save(item);
    }

    @Override
    @Transactional
    public void removeCartItem(Long userId, String signature) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Giỏ hàng không tồn tại"));

        CartItem item = cartItemRepository.findByCartIdAndItemSignature(cart.getId(), signature)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại trong giỏ"));

        cartItemRepository.delete(item);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        // 1. Tìm giỏ hàng của User. Nếu không có thì không cần làm gì cả.
        Optional<Cart> optionalCart = cartRepository.findByUserId(userId);

        if (optionalCart.isPresent()) {
            Cart cart = optionalCart.get();

            // 2. Lấy toàn bộ danh sách món ăn trong giỏ
            List<CartItem> items = cartItemRepository.findByCartId(cart.getId());

            // 3. Xóa sạch sẽ. (Nhờ CascadeType.ALL, bảng Topping sẽ tự động bị xóa theo)
            if (!items.isEmpty()) {
                cartItemRepository.deleteAll(items);
            }

            // LƯU Ý: Tuyệt đối KHÔNG xóa cái "Cart" gốc (cartRepository.delete(cart)),
            // vì cái Cart này giống như cái rổ nhựa. Khách mua xong mình chỉ đổ đồ ra,
            // giữ lại cái rổ rỗng để lần sau khách mua tiếp!
        }
    }
}
