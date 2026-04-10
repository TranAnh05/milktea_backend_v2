package com.example.milktea_backend.services.interfaces;

import com.example.milktea_backend.dtos.responses.ProductDetailResponse;
import com.example.milktea_backend.dtos.responses.ProductResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IProductService {
    List<ProductResponse> getPromotionalProducts();
    ProductDetailResponse getProductDetail(String slug);
    Page<ProductResponse> getProductsByCategorySlug(String slug, int page, int size);
}
