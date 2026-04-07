package com.example.milktea_backend.services.implement;

import com.example.milktea_backend.dtos.responses.ProductDetailResponse;
import com.example.milktea_backend.dtos.responses.ProductResponse;
import com.example.milktea_backend.dtos.responses.SizeDto;
import com.example.milktea_backend.dtos.responses.ToppingDto;
import com.example.milktea_backend.entities.Product;
import com.example.milktea_backend.entities.ProductPromotion;
import com.example.milktea_backend.enums.DiscountType;
import com.example.milktea_backend.repositories.ProductPromotionRepository;
import com.example.milktea_backend.repositories.ProductRepository;
import com.example.milktea_backend.repositories.ProductSizeRepository;
import com.example.milktea_backend.repositories.ToppingRepository;
import com.example.milktea_backend.services.interfaces.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements IProductService {

    private final ProductPromotionRepository promotionRepository;
    private final ProductRepository productRepository;
    private final ProductSizeRepository productSizeRepository;
    private final ToppingRepository toppingRepository;

    @Override
    public List<ProductResponse> getPromotionalProducts() {
        // 1. Lấy danh sách khuyến mãi hợp lệ tính đến thời điểm hiện tại
        LocalDateTime now = LocalDateTime.now();
        List<ProductPromotion> activePromotions = promotionRepository.findActivePromotions(now);

        // 2. Map dữ liệu và tính toán toán học
        return activePromotions.stream().map(promo -> {
            Product product = promo.getProduct();
            int originalPrice = product.getBasePrice();
            int promoPrice = originalPrice;
            int discountPercent = 0;

            // Xử lý logic theo Loại giảm giá
            if (promo.getDiscountType() == DiscountType .PERCENT) {
                // Ví dụ: Giảm 20% của 50k -> Giá mới = 50k - (50k * 20 / 100) = 40k
                discountPercent = promo.getDiscountValue();
                promoPrice = originalPrice - (originalPrice * discountPercent / 100);
            }
            else if (promo.getDiscountType() == DiscountType.FIXED_AMOUNT) {
                // Ví dụ: Giảm thẳng 15k cho ly 50k -> Giá mới = 35k, Tỉ lệ giảm = (15k / 50k) * 100 = 30%
                int discountAmount = promo.getDiscountValue();
                promoPrice = Math.max(0, originalPrice - discountAmount); // Đảm bảo giá không bị âm
                discountPercent = Math.round((float) discountAmount / originalPrice * 100);
            }

            // 3. Đóng gói vào DTO trả về cho Frontend
            return ProductResponse.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .slug(product.getSlug())
                    .thumbnailUrl(product.getThumbnailUrl())
                    .originalPrice(originalPrice)
                    .promotionalPrice(promoPrice)
                    .discountPercent(discountPercent)
                    .averageRating(product.getAverageRating())
                    .build();

        }).collect(Collectors.toList());
    }

    @Override
    public ProductDetailResponse getProductDetail(String slug) {
        // 1. Lấy thông tin sản phẩm gốc
        Product product = productRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm này!"));

        // 2. Tính toán giá khuyến mãi (Giống logic cũ nhưng áp dụng cho 1 sản phẩm)
        int originalPrice = product.getBasePrice();
        int promoPrice = originalPrice;
        int discountPercent = 0;

        Optional<ProductPromotion> activePromo = promotionRepository.findActivePromotionByProductId(product.getId(), LocalDateTime.now());
        if (activePromo.isPresent()) {
            ProductPromotion promo = activePromo.get();
            if (promo.getDiscountType() == DiscountType.PERCENT) {
                discountPercent = promo.getDiscountValue();
                promoPrice = originalPrice - (originalPrice * discountPercent / 100);
            } else if (promo.getDiscountType() == DiscountType.FIXED_AMOUNT) {
                int discountAmount = promo.getDiscountValue();
                promoPrice = Math.max(0, originalPrice - discountAmount);
                discountPercent = Math.round((float) discountAmount / originalPrice * 100);
            }
        }

        // 3. Lấy danh sách Size của sản phẩm này
        List<SizeDto> sizeDtos = productSizeRepository.findActiveSizesByProductId(product.getId())
                .stream().map(ps -> SizeDto.builder()
                        .id(ps.getSize().getId()) // ID của size gốc để mốt truyền xuống Order
                        .name(ps.getSize().getName())
                        .priceSurcharge(ps.getPriceSurcharge())
                        .build())
                .collect(Collectors.toList());

        // 4. Lấy danh sách Topping hệ thống đang có
        List<ToppingDto> toppingDtos = toppingRepository.findByIsActiveTrue()
                .stream().map(t -> ToppingDto.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .price(t.getPrice())
                        .build())
                .collect(Collectors.toList());

        // 5. Đóng gói và trả về
        return ProductDetailResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .thumbnailUrl(product.getThumbnailUrl())
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .categoryName(product.getCategory().getName())
                .categorySlug(product.getCategory().getSlug())
                .originalPrice(originalPrice)
                .promotionalPrice(promoPrice)
                .discountPercent(discountPercent)
                .sizes(sizeDtos)
                .toppings(toppingDtos)
                .build();
    }
}
