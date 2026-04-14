package com.example.milktea_backend.dtos.responses;

import com.example.milktea_backend.enums.ReviewStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminReviewResponse {
    private Long id;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private Long productId;
    private String productName;
    private Integer rating;
    private String comment;
    private String imageUrl;
    private ReviewStatus status;
    private LocalDateTime createdAt;
}
