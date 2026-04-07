package com.example.milktea_backend.services.interfaces;

import com.example.milktea_backend.dtos.responses.CategoryResponse;
import java.util.List;

public interface ICategoryService {
    List<CategoryResponse> getActiveCategories();
}
