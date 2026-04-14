package com.example.milktea_backend.services.implement;

import com.example.milktea_backend.dtos.requests.AdminVoucherRequest;
import com.example.milktea_backend.dtos.responses.VoucherResponse;
import com.example.milktea_backend.entities.Voucher;
import com.example.milktea_backend.enums.DiscountType;
import com.example.milktea_backend.exceptions.ResourceNotFoundException;
import com.example.milktea_backend.repositories.VoucherRepository;
import com.example.milktea_backend.services.interfaces.IAdminProductService;
import com.example.milktea_backend.services.interfaces.IAdminVoucherService;
import com.example.milktea_backend.utils.ExcelCsvHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminVoucherServiceImpl implements IAdminVoucherService {

    private final VoucherRepository voucherRepository;
    private final ExcelCsvHelper excelCsvHelper;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional(readOnly = true)
    public Page<VoucherResponse> getAllVouchers(String keyword, Boolean isActive, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Voucher> vouchers = voucherRepository.findAllForAdmin(keyword, isActive, pageable);
        return vouchers.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public VoucherResponse createVoucher(AdminVoucherRequest request) {
        if (voucherRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Mã voucher '" + request.getCode() + "' đã tồn tại");
        }
        Voucher voucher = buildVoucherFromRequest(new Voucher(), request);
        return mapToResponse(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public VoucherResponse updateVoucher(Long id, AdminVoucherRequest request) {
        Voucher voucher = findOrThrow(id);
        // Kiểm tra trùng code (trừ chính nó)
        if (!voucher.getCode().equals(request.getCode()) && voucherRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Mã voucher '" + request.getCode() + "' đã được sử dụng");
        }
        return mapToResponse(voucherRepository.save(buildVoucherFromRequest(voucher, request)));
    }

    @Override
    @Transactional
    public void deleteVoucher(Long id) {
        voucherRepository.deleteById(findOrThrow(id).getId());
    }

    @Override
    @Transactional
    public void toggleVoucherStatus(Long id) {
        Voucher voucher = findOrThrow(id);
        voucher.setIsActive(!voucher.getIsActive());
        voucherRepository.save(voucher);
    }

    @Override
    public byte[] exportVouchers(String format) {
        List<Voucher> list = voucherRepository.findAll(Sort.by("createdAt").descending());
        List<String> headers = List.of(
                "Mã voucher", "Loại giảm", "Giá trị giảm",
                "Đơn tối thiểu", "Giảm tối đa", "Số lượng",
                "Bắt đầu", "Kết thúc", "Trạng thái"
        );
        List<List<Object>> rows = new ArrayList<>();
        for (Voucher v : list) {
            rows.add(List.of(
                    v.getCode(), v.getDiscountType().name(), v.getDiscountValue(),
                    v.getMinOrderAmount(), v.getMaxDiscountAmount() != null ? v.getMaxDiscountAmount() : "",
                    v.getQuantity(),
                    v.getStartDate().format(DT_FMT), v.getEndDate().format(DT_FMT),
                    Boolean.TRUE.equals(v.getIsActive()) ? "Đang hoạt động" : "Đã tắt"
            ));
        }
        try {
            return "csv".equalsIgnoreCase(format)
                    ? excelCsvHelper.exportToCsv(headers, rows)
                    : excelCsvHelper.exportToExcel("Voucher", headers, rows);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi xuất file", e);
        }
    }

    @Override
    @Transactional
    public IAdminProductService.ImportResult importVouchers(MultipartFile file) {
        List<Map<String, String>> rawRows;
        try {
            rawRows = excelCsvHelper.isExcelFile(file)
                    ? excelCsvHelper.readExcel(file)
                    : excelCsvHelper.readCsv(file);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi đọc file", e);
        }

        int success = 0, failed = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < rawRows.size(); i++) {
            int rowNum = i + 2;
            Map<String, String> row = rawRows.get(i);
            try {
                String code = row.getOrDefault("Mã voucher", "").trim();
                if (code.isBlank()) throw new IllegalArgumentException("Thiếu mã voucher");

                DiscountType dtype = DiscountType.valueOf(
                        row.getOrDefault("Loại giảm", "PERCENT").trim().toUpperCase());
                int discountValue = Integer.parseInt(row.getOrDefault("Giá trị giảm", "0").trim());
                int quantity      = Integer.parseInt(row.getOrDefault("Số lượng", "0").trim());
                LocalDateTime start = LocalDateTime.parse(row.getOrDefault("Bắt đầu", "").trim(), DT_FMT);
                LocalDateTime end   = LocalDateTime.parse(row.getOrDefault("Kết thúc", "").trim(), DT_FMT);

                Optional<Voucher> existing = voucherRepository.findByCode(code);
                Voucher voucher = existing.orElseGet(Voucher::new);
                voucher.setCode(code);
                voucher.setDiscountType(dtype);
                voucher.setDiscountValue(discountValue);
                voucher.setQuantity(quantity);
                voucher.setStartDate(start);
                voucher.setEndDate(end);
                voucher.setIsActive(true);
                voucherRepository.save(voucher);
                success++;
            } catch (Exception e) {
                failed++;
                errors.add("Dòng " + rowNum + ": " + e.getMessage());
            }
        }
        return new IAdminProductService.ImportResult(success, failed, errors);
    }

    private Voucher buildVoucherFromRequest(Voucher voucher, AdminVoucherRequest req) {
        voucher.setCode(req.getCode().toUpperCase());
        voucher.setDiscountType(DiscountType.valueOf(req.getDiscountType()));
        voucher.setDiscountValue(req.getDiscountValue());
        voucher.setMinOrderAmount(req.getMinOrderAmount() != null ? req.getMinOrderAmount() : 0);
        voucher.setMaxDiscountAmount(req.getMaxDiscountAmount());
        voucher.setQuantity(req.getQuantity());
        voucher.setStartDate(req.getStartDate());
        voucher.setEndDate(req.getEndDate());
        voucher.setIsActive(req.getIsActive());
        return voucher;
    }

    private Voucher findOrThrow(Long id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher ID: " + id));
    }

    private VoucherResponse mapToResponse(Voucher v) {
        String message = v.getDiscountType() == DiscountType.PERCENT
                ? "Đã áp dụng giảm " + v.getDiscountValue() + "%"
                : "Đã áp dụng giảm " + v.getDiscountValue() + "đ";

        return VoucherResponse.builder()
                .id(v.getId())
                .code(v.getCode())
                .discountAmount(v.getDiscountValue())
                .message(message)
                .minOrderAmount(v.getMinOrderAmount())
                .build();
    }
}
