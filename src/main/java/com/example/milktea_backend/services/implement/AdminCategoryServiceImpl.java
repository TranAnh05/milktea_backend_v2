package com.example.milktea_backend.services.implement;

import com.example.milktea_backend.dtos.requests.AdminCategoryRequest;
import com.example.milktea_backend.dtos.responses.CategoryResponse;
import com.example.milktea_backend.entities.Category;
import com.example.milktea_backend.exceptions.ResourceNotFoundException;
import com.example.milktea_backend.repositories.CategoryRepository;
import com.example.milktea_backend.services.interfaces.IAdminCategoryService;
import com.example.milktea_backend.services.interfaces.IAdminProductService;
import com.example.milktea_backend.utils.ExcelCsvHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AdminCategoryServiceImpl implements IAdminCategoryService {

    private final CategoryRepository categoryRepository;
    private final ExcelCsvHelper excelCsvHelper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories(Boolean isActive) {
        List<Category> list = (isActive == null)
                ? categoryRepository.findAll()
                : categoryRepository.findByIsActive(isActive);
        return list.stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        return mapToResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(AdminCategoryRequest request) {
        Category cat = Category.builder()
                .name(request.getName())
                .slug(generateSlug(request.getName()))
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .isActive(request.getIsActive())
                .build();
        return mapToResponse(categoryRepository.save(cat));
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, AdminCategoryRequest request) {
        Category cat = findOrThrow(id);
        if (!cat.getName().equals(request.getName())) {
            cat.setSlug(generateSlug(request.getName()));
        }
        cat.setName(request.getName());
        cat.setDescription(request.getDescription());
        cat.setImageUrl(request.getImageUrl());
        cat.setIsActive(request.getIsActive());
        return mapToResponse(categoryRepository.save(cat));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category cat = findOrThrow(id);
        cat.setIsActive(false);
        categoryRepository.save(cat);
    }

    @Override
    public byte[] exportCategories(String format) {
        List<Category> list = categoryRepository.findAll();
        List<String> headers = List.of("ID", "Tên danh mục", "Slug", "Mô tả", "Ảnh", "Trạng thái");
        List<List<Object>> rows = new ArrayList<>();
        for (Category c : list) {
            rows.add(List.of(
                    c.getId(), c.getName(), c.getSlug(),
                    c.getDescription() != null ? c.getDescription() : "",
                    c.getImageUrl() != null ? c.getImageUrl() : "",
                    Boolean.TRUE.equals(c.getIsActive()) ? "Hiển thị" : "Đã ẩn"
            ));
        }
        try {
            return "csv".equalsIgnoreCase(format)
                    ? excelCsvHelper.exportToCsv(headers, rows)
                    : excelCsvHelper.exportToExcel("Danh mục", headers, rows);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi xuất file", e);
        }
    }

    @Override
    @Transactional
    public IAdminProductService.ImportResult importCategories(MultipartFile file) {
        List<Map<String, String>> rawRows;
        try {
            rawRows = excelCsvHelper.isExcelFile(file)
                    ? excelCsvHelper.readExcel(file)
                    : excelCsvHelper.readCsv(file);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi đọc file", e);
        }

        int success = 0, failed = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < rawRows.size(); i++) {
            int rowNum = i + 2;
            Map<String, String> row = rawRows.get(i);
            try {
                String name = row.getOrDefault("Tên danh mục", "").trim();
                if (name.isBlank()) throw new IllegalArgumentException("Thiếu tên danh mục");

                Optional<Category> existing = categoryRepository.findByName(name);
                if (existing.isPresent()) {
                    // Cập nhật nếu đã tồn tại
                    Category cat = existing.get();
                    cat.setDescription(row.getOrDefault("Mô tả", cat.getDescription()));
                    cat.setImageUrl(row.getOrDefault("Ảnh", cat.getImageUrl()));
                    categoryRepository.save(cat);
                } else {
                    categoryRepository.save(Category.builder()
                            .name(name)
                            .slug(generateSlug(name))
                            .description(row.getOrDefault("Mô tả", ""))
                            .imageUrl(row.getOrDefault("Ảnh", ""))
                            .isActive(true)
                            .build());
                }
                success++;
            } catch (Exception e) {
                failed++;
                errors.add("Dòng " + rowNum + ": " + e.getMessage());
            }
        }
        return new IAdminProductService.ImportResult(success, failed, errors);
    }

    private Category findOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục ID: " + id));
    }

    private CategoryResponse mapToResponse(Category c) {
        CategoryResponse response = new CategoryResponse();
        response.setId(c.getId());
        response.setName(c.getName());
        response.setSlug(c.getSlug());
        response.setDescription(c.getDescription());
        response.setImageUrl(c.getImageUrl());
        return response;
    }

    private String generateSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized)
                .replaceAll("").replaceAll("[đĐ]", "d")
                .toLowerCase().trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-").replaceAll("-+", "-");
    }
}
