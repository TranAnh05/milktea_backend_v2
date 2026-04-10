package com.example.milktea_backend.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {

    @NotBlank(message = "Tên người nhận không được để trống")
    private String customerName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "Số điện thoại không hợp lệ (Phải là số VN hợp lệ)")
    private String phone;

    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    private String address;

    private String note; // Ghi chú có thể để trống, không cần validate

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod; // "COD", "VNPAY", "MOMO"

    private Long voucherId; // Có thể null

    // Dành riêng cho khách vãng lai
    private List<CartItemRequest> guestItems;
}
