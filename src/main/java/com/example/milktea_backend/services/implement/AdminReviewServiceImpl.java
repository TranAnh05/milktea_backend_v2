package com.example.milktea_backend.services.implement;

import com.example.milktea_backend.dtos.responses.AdminReviewResponse;
import com.example.milktea_backend.entities.Review;
import com.example.milktea_backend.enums.ReviewStatus;
import com.example.milktea_backend.exceptions.ResourceNotFoundException;
import com.example.milktea_backend.repositories.ReviewRepository;
import com.example.milktea_backend.services.interfaces.IAdminReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminReviewServiceImpl implements IAdminReviewService {

    private final ReviewRepository reviewRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminReviewResponse> getAllReviews(ReviewStatus status, Long productId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return reviewRepository.findAllForAdmin(status, productId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void approveReview(Long reviewId) {
        Review review = findOrThrow(reviewId);
        review.setStatus(ReviewStatus.APPROVED);
        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void hideReview(Long reviewId) {
        Review review = findOrThrow(reviewId);
        review.setStatus(ReviewStatus.HIDDEN);
        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        reviewRepository.delete(findOrThrow(reviewId));
    }

    private Review findOrThrow(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy review ID: " + id));
    }

    private AdminReviewResponse mapToResponse(Review r) {
        return AdminReviewResponse.builder()
                .id(r.getId())
                .userId(r.getUser().getId())
                .userFullName(r.getUser().getFullName())
                .userEmail(r.getUser().getEmail())
                .productId(r.getProduct().getId())
                .productName(r.getProduct().getName())
                .rating(r.getRating())
                .comment(r.getComment())
                .imageUrl(r.getImageUrl())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
