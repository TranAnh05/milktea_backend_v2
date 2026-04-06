package com.example.milktea_backend.dtos.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// Tính năng cực hay: Nếu trường 'data' bị null, Spring sẽ ẩn nó đi khỏi file JSON trả về
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @Builder.Default
    private int status = 200; // Mặc định là 200 OK

    private String message;   // Thông báo thành công (Ví dụ: "Đăng nhập thành công")

    private T data;           // Dữ liệu thực tế trả về (Có thể là User, List<Product>, Token...)

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
