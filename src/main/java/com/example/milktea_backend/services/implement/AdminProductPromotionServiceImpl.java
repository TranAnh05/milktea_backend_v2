package com.example.milktea_backend.services.implement;

import com.example.milktea_backend.dtos.requests.AdminProductPromotionRequest;
import com.example.milktea_backend.dtos.responses.AdminProductPromotionResponse;
import com.example.milktea_backend.entities.Category;
import com.example.milktea_backend.entities.Product;
import com.example.milktea_backend.entities.ProductPromotion;
import com.example.milktea_backend.enums.DiscountType;
import com.example.milktea_backend.exceptions.ResourceNotFoundException;
import com.example.milktea_backend.repositories.CategoryRepository;
import com.example.milktea_backend.repositories.ProductPromotionRepository;
import com.example.milktea_backend.repositories.ProductRepository;
import com.example.milktea_backend.services.interfaces.IAdminProductPromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminProductPromotionServiceImpl implements IAdminProductPromotionService {

    private final ProductPromotionRepository productPromotionRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminProductPromotionResponse> getAllPromotions(String keyword, Long categoryId, Boolean isActive, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ProductPromotion> promotions = productPromotionRepository.findAllForAdmin(keyword, categoryId, isActive, pageable);
        return promotions.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public int createPromotions(AdminProductPromotionRequest request) {
        DiscountType discountType = parseDiscountType(request.getDiscountType());
        validateRequest(request, discountType);

        List<Product> products = resolveTargetProducts(request);
        if (products.isEmpty()) {
            throw new IllegalArgumentException("Không có sản phẩm hợp lệ để áp dụng khuyến mãi");
        }

        List<Long> productIds = products.stream().map(Product::getId).toList();
        disableOverlappingActivePromotions(productIds, request.getStartDate(), request.getEndDate());

        Boolean isActive = request.getIsActive() == null ? Boolean.TRUE : request.getIsActive();
        List<ProductPromotion> toSave = new ArrayList<>();
        for (Product product : products) {
            toSave.add(ProductPromotion.builder()
                    .product(product)
                    .discountType(discountType)
                    .discountValue(request.getDiscountValue())
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .isActive(isActive)
                    .build());
        }

        return productPromotionRepository.saveAll(toSave).size();
    }

    @Override
    @Transactional
    public void togglePromotion(Long id) {
        ProductPromotion promotion = findOrThrow(id);
        promotion.setIsActive(!Boolean.TRUE.equals(promotion.getIsActive()));
        productPromotionRepository.save(promotion);
    }

    @Override
    @Transactional
    public void deletePromotion(Long id) {
        productPromotionRepository.deleteById(findOrThrow(id).getId());
    }

    private ProductPromotion findOrThrow(Long id) {
        return productPromotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khuyến mãi sản phẩm ID: " + id));
    }

    private DiscountType parseDiscountType(String rawType) {
        try {
            return DiscountType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Loại giảm không hợp lệ. Chỉ chấp nhận PERCENT hoặc FIXED_AMOUNT");
        }
    }

    private void validateRequest(AdminProductPromotionRequest request, DiscountType discountType) {
        if (request.getStartDate() == null || request.getEndDate() == null || !request.getStartDate().isBefore(request.getEndDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc");
        }
        if (request.getDiscountValue() == null || request.getDiscountValue() <= 0) {
            throw new IllegalArgumentException("Giá trị giảm phải lớn hơn 0");
        }
        if (discountType == DiscountType.PERCENT && request.getDiscountValue() > 100) {
            throw new IllegalArgumentException("Giảm theo phần trăm không được vượt quá 100");
        }
    }

    private List<Product> resolveTargetProducts(AdminProductPromotionRequest request) {
        String targetType = request.getTargetType() == null ? "" : request.getTargetType().trim().toUpperCase(Locale.ROOT);

        List<Product> products;
        switch (targetType) {
            case "ALL" -> products = productRepository.findByIsActiveTrue();
            case "CATEGORY" -> {
                List<Long> categoryIds = request.getCategoryIds();
                if (categoryIds == null || categoryIds.isEmpty()) {
                    throw new IllegalArgumentException("Vui lòng chọn ít nhất 1 danh mục");
                }
                List<Category> categories = categoryRepository.findAllById(categoryIds);
                if (categories.isEmpty()) {
                    throw new IllegalArgumentException("Danh mục không hợp lệ");
                }
                List<Long> validCategoryIds = categories.stream()
                        .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                        .map(Category::getId)
                        .toList();
                if (validCategoryIds.isEmpty()) {
                    throw new IllegalArgumentException("Danh mục đã chọn không còn hoạt động");
                }
                products = productRepository.findByCategoryIdInAndIsActiveTrue(validCategoryIds);
            }
            case "PRODUCTS" -> {
                List<Long> productIds = request.getProductIds();
                if (productIds == null || productIds.isEmpty()) {
                    throw new IllegalArgumentException("Vui lòng chọn ít nhất 1 sản phẩm");
                }
                products = productRepository.findByIdInAndIsActiveTrue(productIds);
            }
            default -> throw new IllegalArgumentException("targetType không hợp lệ. Chỉ chấp nhận ALL, CATEGORY, PRODUCTS");
        }

        Map<Long, Product> unique = new LinkedHashMap<>();
        for (Product p : products) {
            if (p != null && p.getId() != null) {
                unique.put(p.getId(), p);
            }
        }
        return unique.values().stream().filter(Objects::nonNull).toList();
    }

    private void disableOverlappingActivePromotions(List<Long> productIds, LocalDateTime startDate, LocalDateTime endDate) {
        List<ProductPromotion> overlaps = productPromotionRepository
                .findOverlappingActivePromotions(productIds, startDate, endDate);
        if (overlaps.isEmpty()) {
            return;
        }
        overlaps.forEach(p -> p.setIsActive(false));
        productPromotionRepository.saveAll(overlaps);
    }

    private AdminProductPromotionResponse mapToResponse(ProductPromotion p) {
        Product product = p.getProduct();
        int basePrice = product.getBasePrice() != null ? product.getBasePrice() : 0;
        int promotionalPrice;

        if (p.getDiscountType() == DiscountType.PERCENT) {
            promotionalPrice = basePrice - (basePrice * p.getDiscountValue() / 100);
        } else {
            promotionalPrice = Math.max(0, basePrice - p.getDiscountValue());
        }

        return AdminProductPromotionResponse.builder()
                .id(p.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productSku(product.getSku())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .basePrice(basePrice)
                .discountType(p.getDiscountType().name())
                .discountValue(p.getDiscountValue())
                .promotionalPrice(Math.max(0, promotionalPrice))
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .isActive(p.getIsActive())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
