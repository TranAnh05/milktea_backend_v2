package com.example.milktea_backend.services.implement;

import com.example.milktea_backend.dtos.responses.CategoryResponse;
import com.example.milktea_backend.repositories.CategoryRepository;
import com.example.milktea_backend.services.interfaces.ICategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements ICategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findByIsActiveTrue().stream()
                .map(category -> CategoryResponse.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .slug(category.getSlug())
                        .description(category.getDescription())
                        .imageUrl(category.getImageUrl())
                        .build())
                .collect(Collectors.toList());
    }
}
