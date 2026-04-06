package com.example.milktea_backend.dtos.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private int status;           // Mã lỗi HTTP (Ví dụ: 400, 404, 500)
    private String message;
    Map<String, String> errors; // Thông báo lỗi ngắn gọn cho User đọc
    private String path;          // Đường dẫn API bị lỗi
    private LocalDateTime timestamp; // Thời gian xảy ra lỗi
}
