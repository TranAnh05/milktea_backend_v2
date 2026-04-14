package com.example.milktea_backend.controllers;

import com.example.milktea_backend.dtos.requests.AdminToppingRequest;
import com.example.milktea_backend.dtos.responses.ApiResponse;
import com.example.milktea_backend.dtos.responses.ToppingDto;
import com.example.milktea_backend.entities.Topping;
import com.example.milktea_backend.exceptions.ResourceNotFoundException;
import com.example.milktea_backend.repositories.ToppingRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Topping ít thay đổi nên không cần Service riêng,
 * gọi thẳng Repository là đủ (thin controller pattern).
 * Nếu nghiệp vụ phức tạp hơn sau này, tách ra IAdminToppingService.
 */
@RestController
@RequestMapping("/api/v1/admin/toppings")
@RequiredArgsConstructor
public class AdminToppingController {

    private final ToppingRepository toppingRepository;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER','ROLE_STAFF')")
    public ResponseEntity<ApiResponse<List<ToppingDto>>> getAll(
            @RequestParam(required = false) Boolean isActive) {

        List<Topping> list = (isActive == null)
                ? toppingRepository.findAll()
                : (isActive ? toppingRepository.findByIsActiveTrue() : toppingRepository.findByIsActiveFalse());

        List<ToppingDto> dtos = list.stream()
                .map(t -> ToppingDto.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .price(t.getPrice())
                        .build())
                .toList();

        return ResponseEntity.ok(ApiResponse.<List<ToppingDto>>builder()
                .data(dtos).build());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<ToppingDto>> create(
            @Valid @RequestBody AdminToppingRequest request) {

        Topping saved = toppingRepository.save(
                Topping.builder()
                        .name(request.getName())
                        .price(request.getPrice())
                        .isActive(request.getIsActive())
                        .build());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ToppingDto>builder()
                        .status(201)
                        .message("Tạo topping thành công")
                        .data(ToppingDto.builder()
                                .id(saved.getId())
                                .name(saved.getName())
                                .price(saved.getPrice())
                                .build())
                        .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<ToppingDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody AdminToppingRequest request) {

        Topping topping = toppingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy topping ID: " + id));

        topping.setName(request.getName());
        topping.setPrice(request.getPrice());
        topping.setIsActive(request.getIsActive());
        Topping saved = toppingRepository.save(topping);

        return ResponseEntity.ok(ApiResponse.<ToppingDto>builder()
                .message("Cập nhật topping thành công")
                .data(ToppingDto.builder()
                        .id(saved.getId())
                        .name(saved.getName())
                        .price(saved.getPrice())
                        .build())
                .build());
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> toggle(@PathVariable Long id) {
        Topping topping = toppingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy topping ID: " + id));
        topping.setIsActive(!topping.getIsActive());
        toppingRepository.save(topping);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Đã đổi trạng thái topping").build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Topping topping = toppingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy topping ID: " + id));
        // Soft delete
        topping.setIsActive(false);
        toppingRepository.save(topping);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Đã ẩn topping").build());
    }
}
