package com.example.milktea_backend.services.implement;

import com.example.milktea_backend.dtos.responses.VoucherResponse;
import com.example.milktea_backend.entities.Voucher;
import com.example.milktea_backend.enums.DiscountType;
import com.example.milktea_backend.repositories.VoucherRepository;
import com.example.milktea_backend.services.interfaces.IVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements IVoucherService {

    private final VoucherRepository voucherRepository;

    @Override
    public VoucherResponse checkVoucher(String code, Integer orderValue) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Mã giảm giá không được để trống.");
        }
        if (orderValue == null || orderValue <= 0) {
            throw new IllegalArgumentException("Giá trị đơn hàng không hợp lệ.");
        }

        // Chuẩn hóa code: Cắt khoảng trắng và viết hoa toàn bộ
        String cleanCode = code.trim().toUpperCase();

        // Lớp 1: Tồn tại không?
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(cleanCode)
                .orElseThrow(() -> new IllegalArgumentException("Mã giảm giá không tồn tại."));

        // Lớp 2: Trạng thái kích hoạt?
        if (!voucher.getIsActive()) {
            throw new IllegalArgumentException("Mã giảm giá này đã bị khóa.");
        }

        // Lớp 3: Thời hạn sử dụng?
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getStartDate()) || now.isAfter(voucher.getEndDate())) {
            throw new IllegalArgumentException("Mã giảm giá chưa bắt đầu hoặc đã hết hạn.");
        }

        // Lớp 4: Lượt sử dụng?
        if (voucher.getQuantity() <= 0) {
            throw new IllegalArgumentException("Mã giảm giá đã hết lượt sử dụng.");
        }

        // Lớp 5: Điều kiện giá trị đơn hàng tối thiểu?
        if (orderValue < voucher.getMinOrderAmount()) {
            throw new IllegalArgumentException(
                    "Đơn hàng chưa đạt mức tối thiểu " + String.format("%,d", voucher.getMinOrderAmount()) + "đ để áp dụng mã này."
            );
        }

        // --- TÍNH TOÁN TIỀN GIẢM ---
        int discountAmount = 0;

        if (voucher.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discountAmount = voucher.getDiscountValue();
        } else if (voucher.getDiscountType() == DiscountType.PERCENT) {
            // Tính phần trăm
            discountAmount = (orderValue * voucher.getDiscountValue()) / 100;

            // Ép trần (Capping) nếu có giới hạn mức giảm tối đa
            if (voucher.getMaxDiscountAmount() != null && discountAmount > voucher.getMaxDiscountAmount()) {
                discountAmount = voucher.getMaxDiscountAmount();
            }
        }

        // --- TRẢ KẾT QUẢ ---
        return VoucherResponse.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .discountType(voucher.getDiscountType().name())
                .discountValue(voucher.getDiscountValue())
                .discountAmount(discountAmount)
                .message("Áp dụng mã thành công! Bạn được giảm " + String.format("%,d", discountAmount) + "đ")
                .minOrderAmount(voucher.getMinOrderAmount())
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .build();
    }

    @Override
    public List<VoucherResponse> getActiveVouchers() {
        List<Voucher> activeVouchers = voucherRepository.findActiveVouchers(LocalDateTime.now());

        return activeVouchers.stream().map(v -> {
            String description = v.getDiscountType() == DiscountType.FIXED_AMOUNT
                    ? "Giảm thẳng " + String.format("%,d", v.getDiscountValue()) + "đ"
                    : "Giảm " + v.getDiscountValue() + "%" +
                    (v.getMaxDiscountAmount() != null ? " (Tối đa " + String.format("%,d", v.getMaxDiscountAmount()) + "đ)" : "");

            return VoucherResponse.builder()
                    .id(v.getId())
                    .code(v.getCode())
                    .discountType(v.getDiscountType().name())
                    .discountValue(v.getDiscountValue())
                    .discountAmount(0) // Giá trị này chỉ dùng khi check 1 đơn cụ thể
                    .message(description)
                    .minOrderAmount(v.getMinOrderAmount())
                    .startDate(v.getStartDate())
                    .endDate(v.getEndDate())
                    .build();
        }).toList();
    }
}
