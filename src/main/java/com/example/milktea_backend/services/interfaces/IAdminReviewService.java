package com.example.milktea_backend.services.interfaces;

import com.example.milktea_backend.dtos.responses.AdminReviewResponse;
import com.example.milktea_backend.enums.ReviewStatus;
import org.springframework.data.domain.Page;

public interface IAdminReviewService {

    Page<AdminReviewResponse> getAllReviews(ReviewStatus status, Long productId, int page, int size);

    void approveReview(Long reviewId);

    void hideReview(Long reviewId);

    void deleteReview(Long reviewId);
}
