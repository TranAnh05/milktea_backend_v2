package com.example.milktea_backend.repositories;

import com.example.milktea_backend.entities.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    // ===== PHƯƠNG THỨC CŨ (GIỮ NGUYÊN CHO CLIENT) =====

    @Query("SELECT v FROM Voucher v WHERE v.isActive = true " +
           "AND v.startDate <= :now AND v.endDate >= :now AND v.quantity > 0")
    List<Voucher> findActiveVouchers(@Param("now") LocalDateTime now);

    Optional<Voucher> findByCodeAndIsActiveTrue(String code);

    // ===== PHƯƠNG THỨC MỚI CHO ADMIN =====

    Optional<Voucher> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT v FROM Voucher v WHERE " +
           "(:keyword IS NULL OR v.code LIKE %:keyword%) AND " +
           "(:isActive IS NULL OR v.isActive = :isActive)")
    Page<Voucher> findAllForAdmin(
            @Param("keyword") String keyword,
            @Param("isActive") Boolean isActive,
            Pageable pageable);
}
