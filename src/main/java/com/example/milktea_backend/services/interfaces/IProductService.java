package com.example.milktea_backend.services.interfaces;

import com.example.milktea_backend.dtos.responses.ProductDetailResponse;
import com.example.milktea_backend.dtos.responses.ProductResponse;

import java.util.List;

public interface IProductService {
    List<ProductResponse> getPromotionalProducts();
    ProductDetailResponse getProductDetail(String slug);
}
