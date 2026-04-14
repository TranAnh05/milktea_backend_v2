package com.example.milktea_backend.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Helper dùng chung cho cả Export (Excel/CSV) và Import (Excel/CSV).
 * Dependency cần thêm vào pom.xml:
 *   <dependency>
 *     <groupId>org.apache.poi</groupId>
 *     <artifactId>poi-ooxml</artifactId>
 *     <version>5.2.5</version>
 *   </dependency>
 */
@Component
public class ExcelCsvHelper {

    // =====================================================================
    //  EXPORT
    // =====================================================================

    /**
     * Tạo file Excel (.xlsx) từ headers + rows dạng List<List<Object>>
     */
    public byte[] exportToExcel(String sheetName, List<String> headers, List<List<Object>> rows) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(sheetName);

            // Style cho header
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // Tạo dòng header
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256); // 20 ký tự
            }

            // Tạo dòng dữ liệu
            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                List<Object> rowData = rows.get(r);
                for (int c = 0; c < rowData.size(); c++) {
                    Cell cell = row.createCell(c);
                    Object val = rowData.get(c);
                    if (val == null) {
                        cell.setCellValue("");
                    } else if (val instanceof Number) {
                        cell.setCellValue(((Number) val).doubleValue());
                    } else if (val instanceof Boolean) {
                        cell.setCellValue((Boolean) val);
                    } else {
                        cell.setCellValue(val.toString());
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Tạo file CSV từ headers + rows
     */
    public byte[] exportToCsv(List<String> headers, List<List<Object>> rows) {
        StringBuilder sb = new StringBuilder();
        // BOM UTF-8 để Excel mở đúng tiếng Việt
        sb.append('\uFEFF');
        sb.append(String.join(",", headers)).append("\n");
        for (List<Object> row : rows) {
            List<String> cells = new ArrayList<>();
            for (Object val : row) {
                String s = val == null ? "" : val.toString();
                // Escape dấu phẩy và nháy kép
                if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
                    s = "\"" + s.replace("\"", "\"\"") + "\"";
                }
                cells.add(s);
            }
            sb.append(String.join(",", cells)).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // =====================================================================
    //  IMPORT
    // =====================================================================

    public boolean isExcelFile(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name != null && (name.endsWith(".xlsx") || name.endsWith(".xls"));
    }

    public boolean isCsvFile(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name != null && name.endsWith(".csv");
    }

    /**
     * Đọc file Excel → List<Map<header, value>>
     * Row 0 = header, Row 1+ = dữ liệu
     */
    public List<Map<String, String>> readExcel(MultipartFile file) throws IOException {
        List<Map<String, String>> result = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return result;

            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue().trim());
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                Map<String, String> rowMap = new LinkedHashMap<>();
                boolean allEmpty = true;
                for (int c = 0; c < headers.size(); c++) {
                    Cell cell = row.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    String val = getCellValueAsString(cell);
                    if (!val.isBlank()) allEmpty = false;
                    rowMap.put(headers.get(c), val);
                }
                if (!allEmpty) result.add(rowMap);
            }
        }
        return result;
    }

    /**
     * Đọc file CSV → List<Map<header, value>>
     */
    public List<Map<String, String>> readCsv(MultipartFile file) throws IOException {
        List<Map<String, String>> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) return result;
            // Xóa BOM nếu có
            headerLine = headerLine.replace("\uFEFF", "");
            String[] headers = parseCsvLine(headerLine);

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] values = parseCsvLine(line);
                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    rowMap.put(headers[i].trim(), i < values.length ? values[i].trim() : "");
                }
                result.add(rowMap);
            }
        }
        return result;
    }

    // =====================================================================
    //  PRIVATE HELPERS
    // =====================================================================

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                // Nếu là số nguyên (không có phần thập phân)
                yield d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default      -> "";
        };
    }

    /** CSV parser đơn giản hỗ trợ trường có nháy kép */
    private String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens.toArray(new String[0]);
    }
}
