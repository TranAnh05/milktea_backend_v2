package com.example.milktea_backend.enums;

/**
 * Đồng bộ với DB schema:
 *   PENDING → CONFIRMED → PREPARING → DELIVERING → COMPLETED
 *                                               ↘ CANCELLED (từ bất kỳ bước nào)
 *
 * Lưu ý: Trước đây enum thiếu CONFIRMED so với DB,
 * đã bổ sung để AdminOrderServiceImpl validate đúng luồng.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,    // ← Bổ sung mới
    PREPARING,
    DELIVERING,
    COMPLETED,
    CANCELLED
}
