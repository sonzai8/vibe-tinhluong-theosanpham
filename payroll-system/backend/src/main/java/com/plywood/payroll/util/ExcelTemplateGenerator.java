package com.plywood.payroll.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ExcelTemplateGenerator {

    public static void main(String[] args) throws IOException {
        String importPath = "src/main/resources/templates/attendance/import/import_template.xlsx";
        String exportPath = "src/main/resources/templates/attendance/export/export_template.xlsx";

        Files.createDirectories(Paths.get("src/main/resources/templates/attendance/import"));
        Files.createDirectories(Paths.get("src/main/resources/templates/attendance/export"));

        generateTemplate(importPath, "MẪU NHẬP CHẤM CÔNG", new String[]{"Mã Nhân Viên", "Họ và Tên", "Ngày (YYYY-MM-DD)", "Tên Tổ Biên Chế", "Tên Tổ Thực Tế"});
        generateTemplate(exportPath, "BÁO CÁO CHẤM CÔNG", new String[]{"Mã Nhân Viên", "Họ và Tên", "Ngày", "Tổ Biên Chế", "Tổ Thực Tế", "Ghi Chú"});

        System.out.println("Templates generated successfully!");
    }

    private static void generateTemplate(String path, String title, String[] headers) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); FileOutputStream fileOut = new FileOutputStream(path)) {
            Sheet sheet = workbook.createSheet("Template");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Row 0: Title
            // Row 1: Headers
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Sample data row for import
            if (path.contains("import")) {
                Row sampleRow = sheet.createRow(1);
                sampleRow.createCell(0).setCellValue("NV001");
                sampleRow.createCell(1).setCellValue("Nguyễn Văn A");
                sampleRow.createCell(2).setCellValue("2024-03-01");
                sampleRow.createCell(3).setCellValue("Tổ 1");
                sampleRow.createCell(4).setCellValue("Tổ 1");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(fileOut);
        }
    }
}
