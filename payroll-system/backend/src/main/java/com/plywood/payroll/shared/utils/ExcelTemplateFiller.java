package com.plywood.payroll.shared.utils;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ExcelTemplateFiller {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    /**
     * Điền dữ liệu vào file Excel template có sẵn.
     * @param templateStream InputStream của file template .xlsx
     * @param singleValues Map chứa các giá trị đơn lẻ (VD: "month" -> 10, "year" -> 2023)
     * @param listData Map chứa danh sách các records (VD: "item" -> List<Map>)
     * @return File Excel dưới dạng byte array
     */
    public byte[] fillTemplate(InputStream templateStream, Map<String, Object> singleValues, Map<String, List<Map<String, Object>>> listData) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(templateStream);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                System.out.println("Processing sheet: " + sheet.getSheetName() + ", singleValues: " + (singleValues != null ? singleValues.size() : 0) + ", listData keys: " + (listData != null ? listData.keySet() : "null"));
                processSheet(sheet, singleValues, listData);
            }

            // Xóa các cột/dòng rỗng nếu đang dùng biểu mẫu ma trận có cột dư
            cleanupUnusedPlaceholders(workbook);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Xuất file Excel nhiều sheet dựa trên một sheet mẫu duy nhất.
     * Thường dùng cho: In phiếu lương (mối người 1 sheet), In ma trận tổ (mỗi tổ 1 sheet).
     */
    public byte[] fillMultiSheetTemplate(InputStream templateStream, List<Map<String, Object>> sheetDataList, String sheetNameKey) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(templateStream);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet baseSheet = workbook.getSheetAt(0);

            for (Map<String, Object> data : sheetDataList) {
                String targetName = data.getOrDefault(sheetNameKey, "Sheet").toString()
                        .replaceAll("[\\\\/:*?\"\\[\\]]", "_");
                
                // Tránh trùng tên sheet
                String finalName = targetName;
                int suffix = 1;
                while (workbook.getSheet(finalName) != null) {
                    finalName = targetName + "_" + suffix++;
                }
                if (finalName.length() > 31) finalName = finalName.substring(0, 31);

                Sheet newSheet = workbook.cloneSheet(0);
                workbook.setSheetName(workbook.getSheetIndex(newSheet), finalName);
                
                // Truyền data vào xử lý như single values cho sheet này
                @SuppressWarnings("unchecked")
                Map<String, List<Map<String, Object>>> lists = (Map<String, List<Map<String, Object>>>) data.get("_lists");
                processSheet(newSheet, data, lists);
            }

            // Xóa sheet mẫu ban đầu
            workbook.removeSheetAt(workbook.getSheetIndex(baseSheet));

            cleanupUnusedPlaceholders(workbook);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void processSheet(Sheet sheet, Map<String, Object> singleValues, Map<String, List<Map<String, Object>>> listData) {
        // 1. Thay thế single values trước định dạng list (hoặc làm đồng thời)
        // 2. Tìm kiếm danh sách. Do POI không có template engine tích hợp, ta scan từng row.
        // Giả sử chỉ có 1 list / data prefix được chèn vào 1 sheet cho đơn giản hóa:
        
        int lastRowNum = sheet.getLastRowNum();
        for (int r = 0; r <= lastRowNum; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String listPrefixFound = null;
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.STRING) {
                    String value = cell.getStringCellValue();
                    Matcher m = PLACEHOLDER_PATTERN.matcher(value);
                    if (m.find()) {
                        String key = m.group(1);
                        if (key.contains(".")) {
                            String prefix = key.split("\\.")[0];
                            if (listData != null && listData.containsKey(prefix)) {
                                listPrefixFound = prefix;
                                break;
                            }
                        }
                    }
                }
            }

            if (listPrefixFound != null && listData != null) {
                // Đã tìm thấy dòng template cho list.
                List<Map<String, Object>> items = listData.get(listPrefixFound);
                if (items == null || items.isEmpty()) {
                    System.out.println("No data for list prefix: " + listPrefixFound + ", removing row " + r);
                    sheet.removeRow(row);
                } else {
                    System.out.println("Found list prefix: " + listPrefixFound + " at row " + r + ", items: " + items.size());
                    int insertAt = r;
                    // Copy dòng mẫu ra (items.size() - 1) lần
                    if (items.size() > 1) {
                        int lastRow = sheet.getLastRowNum();
                        if (r < lastRow) {
                            sheet.shiftRows(r + 1, lastRow, items.size() - 1, true, false);
                        }
                        for (int i = 1; i < items.size(); i++) {
                            Row newRow = sheet.createRow(r + i);
                            copyRow(row, newRow);
                        }
                    }

                    // Điền dữ liệu
                    for (int i = 0; i < items.size(); i++) {
                        Row targetRow = sheet.getRow(insertAt + i);
                        Map<String, Object> itemData = items.get(i);
                        replacePlaceholdersInRow(targetRow, listPrefixFound, itemData, singleValues);
                    }
                    
                    // Cập nhật lại biến chạy vòng lặp vì ta đã chèn thêm n dòng
                    r += items.size() - 1;
                    lastRowNum = sheet.getLastRowNum();
                }
            } else {
                // Không phải list, chỉ replace single values
                replacePlaceholdersInRow(row, null, null, singleValues);
            }
        }
    }

    private void replacePlaceholdersInRow(Row row, String listPrefix, Map<String, Object> itemData, Map<String, Object> singleValues) {
        if (row == null) return;
        for (Cell cell : row) {
            if (cell.getCellType() == CellType.STRING) {
                String text = cell.getStringCellValue();
                Matcher m = PLACEHOLDER_PATTERN.matcher(text);
                StringBuffer sb = new StringBuffer();
                boolean replaced = false;
                Object lastReplacement = null;
                boolean isFullReplacement = false;

                while (m.find()) {
                    String key = m.group(1);
                    replaced = true;
                    if (listPrefix != null && key.startsWith(listPrefix + ".")) {
                        String prop = key.substring(listPrefix.length() + 1);
                        lastReplacement = itemData != null ? itemData.get(prop) : null;
                    } else {
                        lastReplacement = singleValues != null ? singleValues.get(key) : null;
                    }
                    String replacementStr = "";
                    if (lastReplacement != null) {
                        replacementStr = lastReplacement.toString();
                    }
                    
                    if (text.trim().equals("${" + key + "}")) {
                        isFullReplacement = true;
                    }
                    m.appendReplacement(sb, Matcher.quoteReplacement(replacementStr));
                }
                
                if (replaced) {
                    m.appendTail(sb);
                    if (isFullReplacement && lastReplacement instanceof Number) {
                        cell.setCellValue(((Number) lastReplacement).doubleValue());
                    } else {
                        cell.setCellValue(sb.toString().trim().isEmpty() ? "" : sb.toString());
                    }
                }
            }
        }
    }

    private void copyRow(Row sourceRow, Row newRow) {
        newRow.setHeight(sourceRow.getHeight());
        for (int i = sourceRow.getFirstCellNum(); i < sourceRow.getLastCellNum(); i++) {
            Cell oldCell = sourceRow.getCell(i);
            Cell newCell = newRow.createCell(i);
            if (oldCell != null) {
                newCell.setCellStyle(oldCell.getCellStyle());
                if (oldCell.getCellType() == CellType.STRING) {
                    newCell.setCellValue(oldCell.getStringCellValue());
                } else if (oldCell.getCellType() == CellType.NUMERIC) {
                    newCell.setCellValue(oldCell.getNumericCellValue());
                } else if (oldCell.getCellType() == CellType.BOOLEAN) {
                    newCell.setCellValue(oldCell.getBooleanCellValue());
                } else if (oldCell.getCellType() == CellType.FORMULA) {
                    newCell.setCellFormula(oldCell.getCellFormula());
                }
            }
        }
    }

    private void cleanupUnusedPlaceholders(Workbook workbook) {
        // Iterate and remove cells/columns that still have ${...} 
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.STRING) {
                        String text = cell.getStringCellValue();
                        if (text != null && text.contains("${")) {
                            cell.setCellValue(""); // clear untouched placeholders
                        }
                    }
                }
            }
        }
    }
}
