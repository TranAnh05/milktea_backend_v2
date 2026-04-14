package com.example.milktea_backend.services.interfaces;

import com.example.milktea_backend.dtos.requests.AdminProductRequest;
import com.example.milktea_backend.dtos.responses.AdminProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IAdminProductService {

    Page<AdminProductResponse> getAllProducts(String keyword, Long categoryId, Boolean isActive, int page, int size);

    AdminProductResponse getProductById(Long id);

    AdminProductResponse createProduct(AdminProductRequest request);

    AdminProductResponse updateProduct(Long id, AdminProductRequest request);

    void deleteProduct(Long id);         // Soft delete: isActive = false

    void restoreProduct(Long id);        // Khôi phục: isActive = true

    // Export danh sách sản phẩm
    byte[] exportProducts(String format); // "excel" | "csv"

    // Import sản phẩm từ file Excel/CSV
    // Trả về số dòng thành công / thất bại
    ImportResult importProducts(MultipartFile file);

    record ImportResult(int success, int failed, List<String> errors) {}
}
