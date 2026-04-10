package com.example.milktea_backend.services.interfaces;

import com.example.milktea_backend.dtos.responses.VoucherResponse;

import java.util.List;

public interface IVoucherService {
    VoucherResponse checkVoucher(String code, Integer orderValue);
    List<VoucherResponse> getActiveVouchers();
}
