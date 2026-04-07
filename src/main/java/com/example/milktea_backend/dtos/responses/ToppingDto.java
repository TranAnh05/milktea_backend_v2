package com.example.milktea_backend.dtos.responses;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ToppingDto {
    private Long id;
    private String name; // VD: "Trân châu đen", "Pudding"
    private Integer price; // VD: 5000, 8000
}
