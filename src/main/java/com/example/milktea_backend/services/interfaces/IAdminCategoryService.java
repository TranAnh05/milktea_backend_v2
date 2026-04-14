package com.example.milktea_backend.services.interfaces;

import com.example.milktea_backend.dtos.requests.AdminCategoryRequest;
import com.example.milktea_backend.dtos.responses.CategoryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IAdminCategoryService {

    List<CategoryResponse> getAllCategories(Boolean isActive);

    CategoryResponse getCategoryById(Long id);

    CategoryResponse createCategory(AdminCategoryRequest request);

    CategoryResponse updateCategory(Long id, AdminCategoryRequest request);

    void deleteCategory(Long id);   // Soft delete

    // Export/Import danh mục
    byte[] exportCategories(String format);

    IAdminProductService.ImportResult importCategories(MultipartFile file);
}
