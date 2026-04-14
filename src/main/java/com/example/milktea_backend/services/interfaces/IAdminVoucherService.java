package com.example.milktea_backend.services.interfaces;

import com.example.milktea_backend.dtos.requests.AdminVoucherRequest;
import com.example.milktea_backend.dtos.responses.VoucherResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface IAdminVoucherService {

    Page<VoucherResponse> getAllVouchers(String keyword, Boolean isActive, int page, int size);

    VoucherResponse createVoucher(AdminVoucherRequest request);

    VoucherResponse updateVoucher(Long id, AdminVoucherRequest request);

    void deleteVoucher(Long id);

    void toggleVoucherStatus(Long id);

    byte[] exportVouchers(String format);

    IAdminProductService.ImportResult importVouchers(MultipartFile file);
}
