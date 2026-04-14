package com.example.milktea_backend.dtos.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminToppingRequest {

    @NotBlank(message = "Tên topping không được để trống")
    private String name;

    @NotNull(message = "Giá không được để trống")
    @Min(value = 0, message = "Giá phải >= 0")
    private Integer price;

    private Boolean isActive = true;
}
