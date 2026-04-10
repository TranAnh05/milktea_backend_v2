package com.example.milktea_backend.repositories;

import com.example.milktea_backend.entities.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {
    void deleteByOrderId(String orderId);
}
