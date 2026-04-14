package com.example.milktea_backend.repositories;

import com.example.milktea_backend.entities.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {

    // ===== PHƯƠNG THỨC CŨ (GIỮ NGUYÊN) =====
    // (giả sử hiện có deleteByOrderId - nếu chưa có thì đây là lần đầu khai báo)
    @Modifying
    @Transactional
    @Query("DELETE FROM VoucherUsage vu WHERE vu.order.id = :orderId")
    void deleteByOrderId(@Param("orderId") String orderId);
}
