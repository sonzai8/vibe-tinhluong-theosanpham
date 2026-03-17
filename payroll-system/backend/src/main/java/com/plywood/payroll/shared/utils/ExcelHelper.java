package com.plywood.payroll.shared.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.apache.poi.ss.util.CellRangeAddressList;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ExcelHelper {
    public static String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    public static byte[] createWorkbookBytes(Workbook workbook) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public static Workbook createWorkbook(InputStream is) throws IOException {
        return new XSSFWorkbook(is);
    }

    public static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    public static void addDataValidation(Sheet sheet, String[] options, int firstRow, int lastRow, int firstCol, int lastCol) {
        if (options == null || options.length == 0) return;
        
        DataValidationHelper validationHelper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = validationHelper.createExplicitListConstraint(options);
        CellRangeAddressList addressList = new CellRangeAddressList(firstRow, lastRow, firstCol, lastCol);
        DataValidation validation = validationHelper.createValidation(constraint, addressList);
        
        // Show dropdown arrow
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);
        
        sheet.addValidationData(validation);
    }
}
