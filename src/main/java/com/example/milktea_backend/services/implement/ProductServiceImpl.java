package com.example.milktea_backend.services.implement;

import com.example.milktea_backend.dtos.responses.ProductResponse;
import com.example.milktea_backend.entities.Product;
import com.example.milktea_backend.entities.ProductPromotion;
import com.example.milktea_backend.enums.DiscountType;
import com.example.milktea_backend.repositories.ProductPromotionRepository;
import com.example.milktea_backend.services.interfaces.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements IProductService {

    private final ProductPromotionRepository promotionRepository;

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
}
