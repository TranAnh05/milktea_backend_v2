package com.example.milktea_backend.services.implement;

import com.example.milktea_backend.dtos.requests.AdminOrderStatusRequest;
import com.example.milktea_backend.dtos.responses.AdminOrderResponse;
import com.example.milktea_backend.dtos.responses.OrderDetailResponse;
import com.example.milktea_backend.entities.Order;
import com.example.milktea_backend.entities.OrderItem;
import com.example.milktea_backend.entities.OrderItemTopping;
import com.example.milktea_backend.enums.OrderStatus;
import com.example.milktea_backend.exceptions.ResourceNotFoundException;
import com.example.milktea_backend.repositories.OrderRepository;
import com.example.milktea_backend.services.interfaces.IAdminOrderService;
import com.example.milktea_backend.utils.ExcelCsvHelper;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements IAdminOrderService {

    private final OrderRepository orderRepository;
    private final ExcelCsvHelper excelCsvHelper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // =====================================================================
    //  DANH SÁCH ĐƠN HÀNG
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<AdminOrderResponse> getAllOrders(
            String keyword, OrderStatus status,
            LocalDateTime from, LocalDateTime to,
            int page, int size, String sortBy, String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        String kw = (keyword != null && !keyword.isBlank()) ? keyword : null;

        Page<Order> orders = orderRepository.findAllForAdmin(status, from, to, kw, pageable);
        return orders.map(this::mapToAdminOrderResponse);
    }

    // =====================================================================
    //  CHI TIẾT ĐƠN HÀNG (DÙNG LẠI mapToOrderDetailResponse của client)
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));
        return mapToOrderDetailResponse(order);
    }

    // =====================================================================
    //  CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG
    // =====================================================================

    @Override
    @Transactional
    public void updateOrderStatus(String orderId, AdminOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));

        OrderStatus newStatus = OrderStatus.valueOf(request.getOrderStatus());

        // Kiểm tra logic chuyển trạng thái hợp lệ
        validateStatusTransition(order.getOrderStatus(), newStatus);

        order.setOrderStatus(newStatus);
        if (newStatus == OrderStatus.CANCELLED && request.getCancelReason() != null) {
            order.setCancelReason(request.getCancelReason());
        }
        orderRepository.save(order);
    }

    // =====================================================================
    //  EXPORT
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public byte[] exportOrders(LocalDateTime from, LocalDateTime to, OrderStatus status, String format) {
        List<AdminOrderResponse> orders = getOrdersForExport(from, to, status);

        if ("pdf".equalsIgnoreCase(format)) {
            return exportOrdersToPdf(orders);
        }

        List<String> headers = List.of(
                "Mã đơn", "Khách hàng", "Số điện thoại", "Địa chỉ",
                "Tổng tiền hàng", "Phí ship", "Giảm giá", "Tổng thanh toán",
                "Phương thức TT", "Trạng thái TT", "Trạng thái đơn",
                "Voucher", "Ghi chú", "Ngày đặt"
        );

        List<List<Object>> rows = new ArrayList<>();
        for (AdminOrderResponse o : orders) {
            rows.add(List.of(
                    o.getOrderId(),
                    o.getGuestName(),
                    o.getGuestPhone(),
                    o.getGuestAddress(),
                    o.getSubTotal(),
                    o.getShippingFee(),
                    o.getDiscountAmount(),
                    o.getFinalTotal(),
                    o.getPaymentMethod().name(),
                    o.getPaymentStatus().name(),
                    o.getOrderStatus().name(),
                    o.getVoucherCode() != null ? o.getVoucherCode() : "",
                    o.getNote() != null ? o.getNote() : "",
                    o.getCreatedAt() != null ? o.getCreatedAt().format(DATE_FMT) : ""
            ));
        }

        try {
            if ("csv".equalsIgnoreCase(format)) {
                return excelCsvHelper.exportToCsv(headers, rows);
            } else {
                return excelCsvHelper.exportToExcel("Đơn hàng", headers, rows);
            }
        } catch (IOException e) {
            throw new RuntimeException("Lỗi xuất file: " + e.getMessage(), e);
        }
    }

    private byte[] exportOrdersToPdf(List<AdminOrderResponse> orders) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream stream = new PDPageContentStream(document, page);
            float margin = 40;
            float y = page.getMediaBox().getHeight() - margin;
            float leading = 14;

            stream.setFont(PDType1Font.HELVETICA_BOLD, 14);
            stream.beginText();
            stream.newLineAtOffset(margin, y);
            stream.showText("INVOICE REPORT");
            stream.endText();

            y -= leading * 1.5f;
            stream.setFont(PDType1Font.HELVETICA, 10);
            stream.beginText();
            stream.newLineAtOffset(margin, y);
            stream.showText("Total orders: " + orders.size());
            stream.endText();

            y -= leading * 1.5f;

            for (int i = 0; i < orders.size(); i++) {
                AdminOrderResponse o = orders.get(i);

                if (y < margin + 80) {
                    stream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    stream = new PDPageContentStream(document, page);
                    y = page.getMediaBox().getHeight() - margin;
                }

                List<String> lines = List.of(
                        "#" + (i + 1) + " | Order: " + safe(o.getOrderId()) + " | Date: " + formatDate(o.getCreatedAt()),
                        "Customer: " + safe(o.getGuestName()) + " | Phone: " + safe(o.getGuestPhone()),
                        "Address: " + safe(o.getGuestAddress()),
                        "Total: " + o.getFinalTotal() + " | Payment: " + o.getPaymentMethod().name() + " | Status: " + o.getOrderStatus().name(),
                        "Note: " + truncate(safe(o.getNote()), 95)
                );

                stream.setFont(PDType1Font.HELVETICA, 9);
                for (String line : lines) {
                    stream.beginText();
                    stream.newLineAtOffset(margin, y);
                    stream.showText(truncate(line, 105));
                    stream.endText();
                    y -= leading;
                }

                y -= 4;
                stream.moveTo(margin, y);
                stream.lineTo(page.getMediaBox().getWidth() - margin, y);
                stream.stroke();
                y -= 8;
            }

            stream.close();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi xuất PDF: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminOrderResponse> getOrdersForExport(LocalDateTime from, LocalDateTime to, OrderStatus status) {
        List<Order> orders = orderRepository.findAllForExport(from, to, status);
        return orders.stream().map(this::mapToAdminOrderResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportOrderInvoicePdf(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng: " + orderId));

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            InvoicePdfContext ctx = startInvoicePage(document);

            drawInvoiceHeader(ctx, order);
            drawInvoiceSummary(ctx, order);
            drawInvoiceItems(document, ctx, order);
            drawInvoiceTotals(document, ctx, order);
            drawInvoiceFooter(ctx, order);

            ctx.stream.close();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi in hóa đơn PDF: " + e.getMessage(), e);
        }
    }

    // =====================================================================
    //  PRIVATE HELPERS
    // =====================================================================

    private AdminOrderResponse mapToAdminOrderResponse(Order order) {
        int totalItems = order.getOrderItems().stream()
                .mapToInt(OrderItem::getQuantity).sum();

        return AdminOrderResponse.builder()
                .orderId(order.getId())
                .guestName(order.getGuestName())
                .guestPhone(order.getGuestPhone())
                .guestAddress(order.getGuestAddress())
                .note(order.getNote())
                .subTotal(order.getSubTotal())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .finalTotal(order.getFinalTotal())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .orderStatus(order.getOrderStatus())
                .cancelReason(order.getCancelReason())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .voucherCode(order.getVoucher() != null ? order.getVoucher().getCode() : null)
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .userEmail(order.getUser() != null ? order.getUser().getEmail() : null)
                .totalItemCount(totalItems)
                .build();
    }

    /** Tái sử dụng logic map chi tiết đơn (giống OrderServiceImpl của client) */
    private OrderDetailResponse mapToOrderDetailResponse(Order order) {
        List<OrderDetailResponse.OrderItemDto> itemDtos = order.getOrderItems().stream().map(item -> {
            List<OrderDetailResponse.OrderItemToppingDto> toppingDtos = item.getOrderItemToppings().stream().map(t ->
                    OrderDetailResponse.OrderItemToppingDto.builder()
                            .id(t.getId())
                            .toppingName(t.getToppingName())
                            .toppingPrice(t.getToppingPrice())
                            .build()
            ).toList();

            return OrderDetailResponse.OrderItemDto.builder()
                    .id(item.getId())
                    .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                    .productName(item.getProductName())
                    .productImage(item.getProductImage())
                    .sizeName(item.getSizeName())
                    .sugarLevel(item.getSugarLevel())
                    .iceLevel(item.getIceLevel())
                    .unitPrice(item.getUnitPrice())
                    .quantity(item.getQuantity())
                    .totalPrice(item.getTotalPrice())
                    .toppings(toppingDtos)
                    .build();
        }).toList();

        return OrderDetailResponse.builder()
                .orderId(order.getId())
                .createdAt(order.getCreatedAt())
                .orderStatus(order.getOrderStatus())
                .cancelReason(order.getCancelReason())
                .guestName(order.getGuestName())
                .guestPhone(order.getGuestPhone())
                .guestAddress(order.getGuestAddress())
                .note(order.getNote())
                .subTotal(order.getSubTotal())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .finalTotal(order.getFinalTotal())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .items(itemDtos)
                .build();
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        // Không cho phép quay ngược trạng thái đã hoàn thành / đã hủy
        if (current == OrderStatus.COMPLETED || current == OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Không thể chuyển trạng thái từ " + current + " sang " + next);
        }
        // Luồng hợp lệ: PENDING → CONFIRMED → PREPARING → DELIVERING → COMPLETED | CANCELLED
        boolean valid = switch (current) {
            case PENDING    -> next == OrderStatus.CONFIRMED  || next == OrderStatus.CANCELLED;
            case CONFIRMED  -> next == OrderStatus.PREPARING  || next == OrderStatus.CANCELLED;
            case PREPARING  -> next == OrderStatus.DELIVERING || next == OrderStatus.CANCELLED;
            case DELIVERING -> next == OrderStatus.COMPLETED  || next == OrderStatus.CANCELLED;
            default         -> false;
        };
        if (!valid) {
            throw new IllegalStateException(
                    "Chuyển trạng thái không hợp lệ: " + current + " → " + next);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String truncate(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;
        return value.substring(0, Math.max(0, max - 3)) + "...";
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_FMT);
    }

    private String ascii(String value) {
        if (value == null) return "";
        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[đĐ]", "d");
    }

    private String money(Integer value) {
        return value == null ? "0" : String.format(Locale.ROOT, "%,d", value);
    }

    private InvoicePdfContext startInvoicePage(PDDocument document) throws IOException {
        InvoicePdfContext ctx = new InvoicePdfContext();
        ctx.margin = 40;
        ctx.page = new PDPage(PDRectangle.A4);
        document.addPage(ctx.page);
        ctx.stream = new PDPageContentStream(document, ctx.page);
        ctx.width = ctx.page.getMediaBox().getWidth();
        ctx.height = ctx.page.getMediaBox().getHeight();
        ctx.y = ctx.height - ctx.margin;
        return ctx;
    }

    private void newInvoicePage(PDDocument document, InvoicePdfContext ctx, String subtitle) throws IOException {
        ctx.stream.close();
        ctx.page = new PDPage(PDRectangle.A4);
        document.addPage(ctx.page);
        ctx.stream = new PDPageContentStream(document, ctx.page);
        ctx.width = ctx.page.getMediaBox().getWidth();
        ctx.height = ctx.page.getMediaBox().getHeight();
        ctx.y = ctx.height - ctx.margin;

        ctx.stream.setNonStrokingColor(33, 82, 122);
        ctx.stream.addRect(ctx.margin, ctx.y - 26, ctx.width - (ctx.margin * 2), 26);
        ctx.stream.fill();

        ctx.stream.setNonStrokingColor(255, 255, 255);
        ctx.stream.setFont(PDType1Font.HELVETICA_BOLD, 13);
        drawText(ctx.stream, ascii("INVOICE"), ctx.margin + 12, ctx.y - 18);
        ctx.stream.setFont(PDType1Font.HELVETICA, 8);
        drawText(ctx.stream, ascii(subtitle), ctx.margin + 95, ctx.y - 18);
        ctx.stream.setFont(PDType1Font.HELVETICA, 8);
        drawText(ctx.stream, ascii("Continued page"), ctx.width - ctx.margin - 70, ctx.y - 18);

        ctx.y -= 42;
        ctx.stream.setNonStrokingColor(0, 0, 0);
    }

    private void ensureSpace(PDDocument document, InvoicePdfContext ctx, float needed, String subtitle) throws IOException {
        if (ctx.y < ctx.margin + needed) {
            newInvoicePage(document, ctx, subtitle);
        }
    }

    private void drawInvoiceHeader(InvoicePdfContext ctx, Order order) throws IOException {
        ctx.stream.setNonStrokingColor(33, 82, 122);
        ctx.stream.addRect(ctx.margin, ctx.y - 72, ctx.width - (ctx.margin * 2), 72);
        ctx.stream.fill();

        ctx.stream.setNonStrokingColor(255, 255, 255);
        ctx.stream.setFont(PDType1Font.HELVETICA_BOLD, 18);
        drawText(ctx.stream, ascii("MILKTEA ADMIN"), ctx.margin + 16, ctx.y - 22);
        ctx.stream.setFont(PDType1Font.HELVETICA, 9);
        drawText(ctx.stream, ascii("Single order invoice"), ctx.margin + 16, ctx.y - 38);
        drawText(ctx.stream, ascii("Order: " + safe(order.getId())), ctx.margin + 16, ctx.y - 54);

        ctx.stream.setFont(PDType1Font.HELVETICA_BOLD, 20);
        drawText(ctx.stream, ascii("INVOICE"), ctx.width - ctx.margin - 118, ctx.y - 24);
        ctx.stream.setFont(PDType1Font.HELVETICA, 9);
        drawText(ctx.stream, ascii("Date: " + formatDate(order.getCreatedAt())), ctx.width - ctx.margin - 118, ctx.y - 40);

        ctx.y -= 94;
        ctx.stream.setNonStrokingColor(0, 0, 0);
    }

    private void drawInvoiceSummary(InvoicePdfContext ctx, Order order) throws IOException {
        drawSectionTitle(ctx, "ORDER INFORMATION");

        float leftX = ctx.margin + 12;
        float rightX = ctx.margin + 275;
        float labelWidth = 95;
        float valueWidth = 180;

        ctx.stream.setFont(PDType1Font.HELVETICA, 9);
        drawKeyValue(ctx, leftX, ctx.y, labelWidth, valueWidth, "Order ID", safe(order.getId()));
        drawKeyValue(ctx, rightX, ctx.y, labelWidth, valueWidth, "Payment", order.getPaymentMethod().name());
        ctx.y -= 16;
        drawKeyValue(ctx, leftX, ctx.y, labelWidth, valueWidth, "Customer", safe(order.getGuestName()));
        drawKeyValue(ctx, rightX, ctx.y, labelWidth, valueWidth, "Phone", safe(order.getGuestPhone()));
        ctx.y -= 16;

        if (order.getUser() != null && order.getUser().getEmail() != null) {
            drawKeyValue(ctx, leftX, ctx.y, labelWidth, valueWidth, "Customer email", order.getUser().getEmail());
            drawKeyValue(ctx, rightX, ctx.y, labelWidth, valueWidth, "Created", formatDate(order.getCreatedAt()));
            ctx.y -= 16;
            drawKeyValue(ctx, leftX, ctx.y, labelWidth, valueWidth, "Updated", formatDate(order.getUpdatedAt()));
            ctx.y -= 16;
        }

        ctx.y -= drawWrappedKeyValue(ctx, leftX, ctx.y, labelWidth, valueWidth, "Address", safe(order.getGuestAddress())) + 4;
        if (order.getVoucher() != null && order.getVoucher().getCode() != null) {
            ctx.y -= drawWrappedKeyValue(ctx, leftX, ctx.y, labelWidth, valueWidth, "Voucher", order.getVoucher().getCode()) + 4;
        }
        if (order.getNote() != null && !order.getNote().isBlank()) {
            ctx.y -= drawWrappedKeyValue(ctx, leftX, ctx.y, labelWidth, valueWidth, "Note", order.getNote()) + 4;
        }
        if (order.getCancelReason() != null && !order.getCancelReason().isBlank()) {
            ctx.y -= drawWrappedKeyValue(ctx, leftX, ctx.y, labelWidth, valueWidth, "Cancel reason", order.getCancelReason()) + 4;
        }

        ctx.y -= 10;
        drawHorizontalRule(ctx, ctx.y);
        ctx.y -= 14;
    }

    private void drawInvoiceItems(PDDocument document, InvoicePdfContext ctx, Order order) throws IOException {
        drawSectionTitle(ctx, "ITEM DETAILS");

        if (order.getOrderItems().isEmpty()) {
            ctx.stream.setFont(PDType1Font.HELVETICA, 9);
            drawText(ctx.stream, ascii("No items in this order"), ctx.margin + 12, ctx.y);
            ctx.y -= 20;
            return;
        }

        drawItemsTableHeader(ctx);

        int index = 1;
        for (OrderItem item : order.getOrderItems()) {
            List<String> itemLines = buildItemLines(item);
            float blockHeight = estimateItemBlockHeight(itemLines);
            ensureSpace(document, ctx, blockHeight + 30, "ITEM DETAILS");

            if (ctx.y - blockHeight < ctx.margin + 18) {
                newInvoicePage(document, ctx, "ITEM DETAILS");
                drawItemsTableHeader(ctx);
            }

            float boxTop = ctx.y;
            float boxBottom = ctx.y - blockHeight;
            float boxWidth = ctx.width - (ctx.margin * 2);

            ctx.stream.setNonStrokingColor(248, 250, 252);
            ctx.stream.addRect(ctx.margin, boxBottom, boxWidth, blockHeight);
            ctx.stream.fill();

            ctx.stream.setStrokingColor(214, 219, 223);
            ctx.stream.addRect(ctx.margin, boxBottom, boxWidth, blockHeight);
            ctx.stream.stroke();

            ctx.stream.setNonStrokingColor(0, 0, 0);
            ctx.stream.setFont(PDType1Font.HELVETICA_BOLD, 10);
            float colNo = ctx.margin + 12;
            float colProduct = ctx.margin + 34;
            float colOptions = ctx.margin + 178;
            float colQty = ctx.margin + 366;
            float colUnit = ctx.margin + 410;
            float colTotal = ctx.margin + 484;

            drawText(ctx.stream, ascii(String.valueOf(index)), colNo, boxTop - 15);
            drawText(ctx.stream, ascii(truncate(safe(item.getProductName()), 28)), colProduct, boxTop - 15);
            drawText(ctx.stream, ascii(String.valueOf(item.getQuantity())), colQty, boxTop - 15);
            drawText(ctx.stream, ascii(money(resolveProductPrice(item))), colUnit, boxTop - 15);
            drawText(ctx.stream, ascii(money(item.getTotalPrice())), colTotal, boxTop - 15);

            ctx.stream.setFont(PDType1Font.HELVETICA, 8.5f);
            float textY = boxTop - 28;
            for (String line : itemLines) {
                drawText(ctx.stream, ascii(line), colProduct, textY);
                textY -= 10;
            }

            if (item.getOrderItemToppings() != null && !item.getOrderItemToppings().isEmpty()) {
                textY -= 10;
                ctx.stream.setFont(PDType1Font.HELVETICA_BOLD, 8.2f);
                drawText(ctx.stream, ascii("Toppings:"), colProduct, textY);
                textY -= 10;
                ctx.stream.setFont(PDType1Font.HELVETICA, 8.2f);
                for (OrderItemTopping topping : item.getOrderItemToppings()) {
                    drawText(ctx.stream, ascii("- " + safe(topping.getToppingName()) + " (+" + money(topping.getToppingPrice()) + ")"), colProduct, textY);
                    textY -= 9;
                }
            }

            ctx.y = boxBottom - 10;
            index++;
        }

        ctx.y -= 6;
        drawHorizontalRule(ctx, ctx.y);
        ctx.y -= 14;
    }

    private void drawInvoiceTotals(PDDocument document, InvoicePdfContext ctx, Order order) throws IOException {
        ensureSpace(document, ctx, 112, "SUMMARY");

        float boxWidth = 235;
        float boxHeight = 92;
        float boxX = ctx.width - ctx.margin - boxWidth;
        float boxY = ctx.y - boxHeight;

        ctx.stream.setNonStrokingColor(245, 247, 250);
        ctx.stream.addRect(boxX, boxY, boxWidth, boxHeight);
        ctx.stream.fill();

        ctx.stream.setStrokingColor(214, 219, 223);
        ctx.stream.addRect(boxX, boxY, boxWidth, boxHeight);
        ctx.stream.stroke();

        ctx.stream.setNonStrokingColor(0, 0, 0);
        ctx.stream.setFont(PDType1Font.HELVETICA_BOLD, 10);
        drawText(ctx.stream, ascii("SUMMARY"), boxX + 12, ctx.y - 16);

        ctx.stream.setFont(PDType1Font.HELVETICA, 9);
        drawSummaryLine(ctx, boxX + 12, ctx.y - 32, "Subtotal", money(order.getSubTotal()));
        drawSummaryLine(ctx, boxX + 12, ctx.y - 48, "Shipping", money(order.getShippingFee()));
        drawSummaryLine(ctx, boxX + 12, ctx.y - 64, "Discount", money(order.getDiscountAmount()));

        ctx.stream.setFont(PDType1Font.HELVETICA_BOLD, 11);
        drawSummaryLine(ctx, boxX + 12, ctx.y - 82, "Total", money(order.getFinalTotal()));

        ctx.y = boxY - 18;
    }

    private void drawItemsTableHeader(InvoicePdfContext ctx) throws IOException {
        float headerHeight = 18;
        float headerBottom = ctx.y - headerHeight;
        float boxWidth = ctx.width - (ctx.margin * 2);

        ctx.stream.setNonStrokingColor(33, 82, 122);
        ctx.stream.addRect(ctx.margin, headerBottom, boxWidth, headerHeight);
        ctx.stream.fill();

        ctx.stream.setNonStrokingColor(255, 255, 255);
        ctx.stream.setFont(PDType1Font.HELVETICA_BOLD, 8.2f);
        drawText(ctx.stream, ascii("#"), ctx.margin + 12, ctx.y - 12);
        drawText(ctx.stream, ascii("Product / Options"), ctx.margin + 34, ctx.y - 12);
        drawText(ctx.stream, ascii("Qty"), ctx.margin + 366, ctx.y - 12);
        drawText(ctx.stream, ascii("Unit"), ctx.margin + 410, ctx.y - 12);
        drawText(ctx.stream, ascii("Total"), ctx.margin + 484, ctx.y - 12);

        ctx.stream.setNonStrokingColor(0, 0, 0);
        ctx.y -= 26;
    }

    private void drawInvoiceFooter(InvoicePdfContext ctx, Order order) throws IOException {
        if (ctx.y < ctx.margin + 28) {
            ctx.stream.close();
            return;
        }

        ctx.stream.setStrokingColor(220, 224, 230);
        ctx.stream.moveTo(ctx.margin, ctx.y);
        ctx.stream.lineTo(ctx.width - ctx.margin, ctx.y);
        ctx.stream.stroke();

        ctx.y -= 18;
        ctx.stream.setFont(PDType1Font.HELVETICA_OBLIQUE, 8.5f);
        drawText(ctx.stream, ascii("Thank you for your order"), ctx.margin, ctx.y);
        drawText(ctx.stream, ascii("Generated from order record " + safe(order.getId())), ctx.width - ctx.margin - 150, ctx.y);
    }

    private void drawSectionTitle(InvoicePdfContext ctx, String title) throws IOException {
        ctx.stream.setNonStrokingColor(33, 82, 122);
        ctx.stream.addRect(ctx.margin, ctx.y - 18, 120, 18);
        ctx.stream.fill();
        ctx.stream.setNonStrokingColor(255, 255, 255);
        ctx.stream.setFont(PDType1Font.HELVETICA_BOLD, 9);
        drawText(ctx.stream, ascii(title), ctx.margin + 10, ctx.y - 12);
        ctx.stream.setNonStrokingColor(0, 0, 0);
        ctx.y -= 28;
    }

    private void drawKeyValue(InvoicePdfContext ctx, float x, float y, float labelWidth, float valueWidth, String label, String value) throws IOException {
        ctx.stream.setFont(PDType1Font.HELVETICA_BOLD, 8.5f);
        drawText(ctx.stream, ascii(label + ":"), x, y);
        ctx.stream.setFont(PDType1Font.HELVETICA, 8.5f);
        drawText(ctx.stream, ascii(truncate(value, Math.max(16, (int) valueWidth / 5))), x + labelWidth, y);
    }

    private float drawWrappedKeyValue(InvoicePdfContext ctx, float x, float y, float labelWidth, float valueWidth, String label, String value) throws IOException {
        ctx.stream.setFont(PDType1Font.HELVETICA_BOLD, 8.5f);
        drawText(ctx.stream, ascii(label + ":"), x, y);
        ctx.stream.setFont(PDType1Font.HELVETICA, 8.5f);

        List<String> wrapped = wrapText(ascii(safe(value)), Math.max(18, (int) valueWidth / 4));
        float cursorY = y;
        for (int i = 0; i < wrapped.size(); i++) {
            drawText(ctx.stream, wrapped.get(i), x + labelWidth, cursorY);
            cursorY -= 10;
        }

        return Math.max(10f, wrapped.size() * 10f);
    }

    private void drawSummaryLine(InvoicePdfContext ctx, float x, float y, String label, String value) throws IOException {
        ctx.stream.setFont(PDType1Font.HELVETICA, 9);
        drawText(ctx.stream, ascii(label), x, y);
        drawText(ctx.stream, ascii(value), x + 132, y);
    }

    private void drawHorizontalRule(InvoicePdfContext ctx, float y) throws IOException {
        ctx.stream.setStrokingColor(220, 224, 230);
        ctx.stream.moveTo(ctx.margin, y);
        ctx.stream.lineTo(ctx.width - ctx.margin, y);
        ctx.stream.stroke();
        ctx.stream.setStrokingColor(0, 0, 0);
    }

    private List<String> buildItemLines(OrderItem item) {
        List<String> lines = new ArrayList<>();
        lines.add("Size: " + safe(item.getSizeName()));
        lines.add("Sugar: " + safe(item.getSugarLevel()) + " | Ice: " + safe(item.getIceLevel()));
        lines.add("Product price: " + money(resolveProductPrice(item)));

        return lines;
    }

    private Integer resolveProductPrice(OrderItem item) {
        if (item.getProduct() != null && item.getProduct().getBasePrice() != null) {
            return item.getProduct().getBasePrice();
        }
        return item.getUnitPrice();
    }

    private float estimateItemBlockHeight(List<String> lines) {
        return 44 + (lines.size() * 12f);
    }

    private List<String> wrapText(String value, int maxChars) {
        List<String> lines = new ArrayList<>();
        if (value == null || value.isBlank()) {
            lines.add("");
            return lines;
        }

        String[] words = value.split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (current.isEmpty()) {
                if (word.length() <= maxChars) {
                    current.append(word);
                } else {
                    for (int i = 0; i < word.length(); i += maxChars) {
                        lines.add(word.substring(i, Math.min(word.length(), i + maxChars)));
                    }
                }
                continue;
            }

            if (current.length() + 1 + word.length() <= maxChars) {
                current.append(' ').append(word);
            } else {
                lines.add(current.toString());
                current = new StringBuilder();
                if (word.length() <= maxChars) {
                    current.append(word);
                } else {
                    for (int i = 0; i < word.length(); i += maxChars) {
                        lines.add(word.substring(i, Math.min(word.length(), i + maxChars)));
                    }
                }
            }
        }

        if (!current.isEmpty()) {
            lines.add(current.toString());
        }

        return lines;
    }

    private static class InvoicePdfContext {
        private PDPage page;
        private PDPageContentStream stream;
        private float margin;
        private float width;
        private float height;
        private float y;
    }

    private void drawText(PDPageContentStream stream, String text, float x, float y) throws IOException {
        stream.beginText();
        stream.newLineAtOffset(x, y);
        stream.showText(safe(text));
        stream.endText();
    }
}
