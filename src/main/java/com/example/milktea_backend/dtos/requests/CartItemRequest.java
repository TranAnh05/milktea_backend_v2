package com.example.milktea_backend.dtos.requests;

import lombok.Data;
import java.util.List;

@Data
public class CartItemRequest {
    // Chữ ký độc nhất tạo từ Frontend (VD: "1-2-50%-100%-1_3")
    private String signature;

    private Long productId;
    private Long sizeId;
    private String sugarLevel;
    private String iceLevel;

    // Chỉ cần nhận ID của Topping, Backend sẽ tự móc giá từ Database ra để bảo mật
    private List<Long> toppingIds;

    private Integer quantity;
}
