package com.example.milktea_backend.services.implement;

import com.example.milktea_backend.dtos.requests.AdminProductRequest;
import com.example.milktea_backend.dtos.responses.AdminProductResponse;
import com.example.milktea_backend.dtos.responses.SizeDto;
import com.example.milktea_backend.entities.*;
import com.example.milktea_backend.exceptions.ResourceNotFoundException;
import com.example.milktea_backend.repositories.*;
import com.example.milktea_backend.services.interfaces.IAdminProductService;
import com.example.milktea_backend.utils.ExcelCsvHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AdminProductServiceImpl implements IAdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductSizeRepository productSizeRepository;
    private final SizeRepository sizeRepository;
    private final ExcelCsvHelper excelCsvHelper;

    // =====================================================================
    //  READ
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<AdminProductResponse> getAllProducts(
            String keyword, Long categoryId, Boolean isActive, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        String kw = (keyword != null && !keyword.isBlank()) ? keyword : null;
        Page<Product> products = productRepository.findAllForAdmin(kw, categoryId, isActive, pageable);
        return products.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminProductResponse getProductById(Long id) {
        Product product = findProductOrThrow(id);
        return mapToResponse(product);
    }

    // =====================================================================
    //  CREATE
    // =====================================================================

    @Override
    @Transactional
    public AdminProductResponse createProduct(AdminProductRequest request) {
        // Validate trùng SKU
        if (productRepository.existsBySku(request.getSku())) {
            throw new IllegalArgumentException("SKU '" + request.getSku() + "' đã tồn tại");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));

        String slug = generateUniqueSlug(request.getName(), null);

        Product product = Product.builder()
                .category(category)
                .sku(request.getSku().toUpperCase())
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .thumbnailUrl(request.getThumbnailUrl())
                .isActive(request.getIsActive())
                .build();

        product = productRepository.save(product);

        // Lưu sizes nếu có
        if (request.getSizes() != null) {
            saveProductSizes(product, request.getSizes());
        }

        return mapToResponse(product);
    }

    // =====================================================================
    //  UPDATE
    // =====================================================================

    @Override
    @Transactional
    public AdminProductResponse updateProduct(Long id, AdminProductRequest request) {
        Product product = findProductOrThrow(id);

        // Validate SKU trùng (trừ chính nó)
        if (productRepository.existsBySkuAndIdNot(request.getSku(), id)) {
            throw new IllegalArgumentException("SKU '" + request.getSku() + "' đã được sử dụng");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));

        // Chỉ đổi slug nếu tên thay đổi
        if (!product.getName().equals(request.getName())) {
            product.setSlug(generateUniqueSlug(request.getName(), id));
        }

        product.setCategory(category);
        product.setSku(request.getSku().toUpperCase());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBasePrice(request.getBasePrice());
        product.setThumbnailUrl(request.getThumbnailUrl());
        product.setIsActive(request.getIsActive());
        product = productRepository.save(product);

        // Cập nhật sizes: xóa cũ → thêm mới
        if (request.getSizes() != null) {
            productSizeRepository.deleteByProductId(id);
            saveProductSizes(product, request.getSizes());
        }

        return mapToResponse(product);
    }

    // =====================================================================
    //  DELETE / RESTORE
    // =====================================================================

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = findProductOrThrow(id);
        product.setIsActive(false);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void restoreProduct(Long id) {
        Product product = findProductOrThrow(id);
        product.setIsActive(true);
        productRepository.save(product);
    }

    // =====================================================================
    //  EXPORT
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public byte[] exportProducts(String format) {
        List<Product> all = productRepository.findAll(Sort.by("createdAt").descending());

        List<String> headers = List.of(
                "ID", "SKU", "Tên sản phẩm", "Danh mục",
                "Giá gốc", "Mô tả", "Ảnh thumbnail",
                "Đánh giá TB", "Số review", "Trạng thái"
        );

        List<List<Object>> rows = new ArrayList<>();
        for (Product p : all) {
            rows.add(List.of(
                    p.getId(), p.getSku(), p.getName(),
                    p.getCategory().getName(), p.getBasePrice(),
                    p.getDescription() != null ? p.getDescription() : "",
                    p.getThumbnailUrl() != null ? p.getThumbnailUrl() : "",
                    p.getAverageRating(), p.getReviewCount(),
                    Boolean.TRUE.equals(p.getIsActive()) ? "Đang bán" : "Đã ẩn"
            ));
        }

        try {
            return "csv".equalsIgnoreCase(format)
                    ? excelCsvHelper.exportToCsv(headers, rows)
                    : excelCsvHelper.exportToExcel("Sản phẩm", headers, rows);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi xuất file: " + e.getMessage(), e);
        }
    }

    // =====================================================================
    //  IMPORT
    // =====================================================================

    @Override
    @Transactional
    public ImportResult importProducts(MultipartFile file) {
        List<Map<String, String>> rawRows;
        try {
            if (excelCsvHelper.isExcelFile(file)) {
                rawRows = excelCsvHelper.readExcel(file);
            } else if (excelCsvHelper.isCsvFile(file)) {
                rawRows = excelCsvHelper.readCsv(file);
            } else {
                throw new IllegalArgumentException("Chỉ hỗ trợ file .xlsx, .xls, .csv");
            }
        } catch (IOException e) {
            throw new RuntimeException("Lỗi đọc file: " + e.getMessage(), e);
        }

        int success = 0, failed = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < rawRows.size(); i++) {
            int rowNum = i + 2; // +2 vì row 1 là header
            Map<String, String> row = rawRows.get(i);
            try {
                String sku      = requireField(row, "SKU");
                String name     = requireField(row, "Tên sản phẩm");
                String catName  = requireField(row, "Danh mục");
                String priceStr = requireField(row, "Giá gốc");

                int basePrice = Integer.parseInt(priceStr.replace(",", "").replace(".", "").trim());

                Category category = categoryRepository.findByName(catName)
                        .orElseThrow(() -> new ResourceNotFoundException("Danh mục '" + catName + "' không tồn tại"));

                String normalizedSku = sku.toUpperCase();
                Optional<Product> existing = productRepository.findAll().stream()
                        .filter(p -> normalizedSku.equalsIgnoreCase(p.getSku()))
                        .findFirst();
                Product product;
                if (existing.isPresent()) {
                    product = existing.get();
                    product.setName(name);
                    product.setBasePrice(basePrice);
                    product.setCategory(category);
                    product.setDescription(row.getOrDefault("Mô tả", ""));
                    product.setThumbnailUrl(row.getOrDefault("Ảnh thumbnail", ""));
                } else {
                    product = Product.builder()
                            .sku(normalizedSku)
                            .name(name)
                            .slug(generateUniqueSlug(name, null))
                            .basePrice(basePrice)
                            .category(category)
                            .description(row.getOrDefault("Mô tả", ""))
                            .thumbnailUrl(row.getOrDefault("Ảnh thumbnail", ""))
                            .isActive(true)
                            .build();
                }
                productRepository.save(product);
                success++;

            } catch (Exception e) {
                failed++;
                errors.add("Dòng " + rowNum + ": " + e.getMessage());
            }
        }
        return new ImportResult(success, failed, errors);
    }

    // =====================================================================
    //  PRIVATE HELPERS
    // =====================================================================

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm ID: " + id));
    }

    private AdminProductResponse mapToResponse(Product p) {
        List<SizeDto> sizes = productSizeRepository.findActiveSizesByProductId(p.getId())
                .stream().map(ps -> SizeDto.builder()
                        .id(ps.getSize().getId())
                        .name(ps.getSize().getName())
                        .priceSurcharge(ps.getPriceSurcharge())
                        .build())
                .toList();

        return AdminProductResponse.builder()
                .id(p.getId())
                .sku(p.getSku())
                .name(p.getName())
                .slug(p.getSlug())
                .description(p.getDescription())
                .basePrice(p.getBasePrice())
                .thumbnailUrl(p.getThumbnailUrl())
                .averageRating(p.getAverageRating())
                .reviewCount(p.getReviewCount())
                .isActive(p.getIsActive())
                .categoryId(p.getCategory().getId())
                .categoryName(p.getCategory().getName())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .sizes(sizes)
                .build();
    }

    private void saveProductSizes(Product product, List<AdminProductRequest.ProductSizeEntry> entries) {
        for (AdminProductRequest.ProductSizeEntry entry : entries) {
            Size size = sizeRepository.findById(entry.getSizeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy size ID: " + entry.getSizeId()));
            ProductSize ps = ProductSize.builder()
                    .product(product)
                    .size(size)
                    .priceSurcharge(entry.getPriceSurcharge() != null ? entry.getPriceSurcharge() : 0)
                    .isActive(entry.getIsActive() != null ? entry.getIsActive() : true)
                    .build();
            productSizeRepository.save(ps);
        }
    }

    /** Tạo slug từ tên tiếng Việt, đảm bảo không trùng */
    private String generateUniqueSlug(String name, Long excludeId) {
        String base = toSlug(name);
        String slug = base;
        int count = 1;
        while (excludeId != null
                ? productRepository.existsBySlugAndIdNot(slug, excludeId)
                : productRepository.existsBySlug(slug)) {
            slug = base + "-" + count++;
        }
        return slug;
    }

    private String toSlug(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("")
                .replaceAll("[đĐ]", "d")
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }

    private String requireField(Map<String, String> row, String field) {
        String val = row.get(field);
        if (val == null || val.isBlank()) {
            throw new IllegalArgumentException("Thiếu trường '" + field + "'");
        }
        return val.trim();
    }
}
