package com.example.milktea_backend.services.interfaces;

import com.example.milktea_backend.dtos.requests.AdminProductPromotionRequest;
import com.example.milktea_backend.dtos.responses.AdminProductPromotionResponse;
import org.springframework.data.domain.Page;

public interface IAdminProductPromotionService {

    Page<AdminProductPromotionResponse> getAllPromotions(String keyword, Long categoryId, Boolean isActive, int page, int size);

    int createPromotions(AdminProductPromotionRequest request);

    void togglePromotion(Long id);

    void deletePromotion(Long id);
}
