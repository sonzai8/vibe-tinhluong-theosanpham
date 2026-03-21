package com.plywood.payroll.shared.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExcelTemplateFillerTest {

    private final ExcelTemplateFiller filler = new ExcelTemplateFiller();

    @Test
    void testFillTemplate_SingleValuesAndList() throws IOException {
        // Create template in memory
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("TestSheet");
        
        // Row 0: Single values
        Row row0 = sheet.createRow(0);
        row0.createCell(0).setCellValue("Month: ${month}");
        row0.createCell(1).setCellValue("Year: ${year}");
        
        // Row 2: List template
        Row row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue("${item.name}");
        row2.createCell(1).setCellValue("${item.value}");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        byte[] templateBytes = bos.toByteArray();
        workbook.close();

        // Data for filling
        Map<String, Object> singleValues = new HashMap<>();
        singleValues.put("month", 10);
        singleValues.put("year", 2023);

        List<Map<String, Object>> listItems = new ArrayList<>();
        Map<String, Object> item1 = new HashMap<>();
        item1.put("name", "Alice");
        item1.put("value", 100);
        listItems.add(item1);

        Map<String, Object> item2 = new HashMap<>();
        item2.put("name", "Bob");
        item2.put("value", 200);
        listItems.add(item2);

        Map<String, List<Map<String, Object>>> listData = new HashMap<>();
        listData.put("item", listItems);

        // Execute fill
        InputStream is = new ByteArrayInputStream(templateBytes);
        byte[] resultBytes = filler.fillTemplate(is, singleValues, listData);

        // Verify result
        Workbook resultWorkbook = new XSSFWorkbook(new ByteArrayInputStream(resultBytes));
        Sheet resultSheet = resultWorkbook.getSheetAt(0);

        // Check single values
        assertEquals("Month: 10", resultSheet.getRow(0).getCell(0).getStringCellValue());
        assertEquals("Year: 2023", resultSheet.getRow(0).getCell(1).getStringCellValue());

        // Check list values (row 2 and row 3 since Bob was added)
        assertEquals("Alice", resultSheet.getRow(2).getCell(0).getStringCellValue());
        assertEquals(100.0, resultSheet.getRow(2).getCell(1).getNumericCellValue());
        
        assertEquals("Bob", resultSheet.getRow(3).getCell(0).getStringCellValue());
        assertEquals(200.0, resultSheet.getRow(3).getCell(1).getNumericCellValue());

        resultWorkbook.close();
    }

    @Test
    void testFillMultiSheetTemplate() throws IOException {
        // Create template in memory
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Template");
        Row row = sheet.createRow(0);
        row.createCell(0).setCellValue("Hello ${name}");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        byte[] templateBytes = bos.toByteArray();
        workbook.close();

        // Data for filling
        List<Map<String, Object>> sheetDataList = new ArrayList<>();
        
        Map<String, Object> data1 = new HashMap<>();
        data1.put("sheetName", "SheetA");
        data1.put("name", "Alpha");
        sheetDataList.add(data1);

        Map<String, Object> data2 = new HashMap<>();
        data2.put("sheetName", "SheetB");
        data2.put("name", "Beta");
        sheetDataList.add(data2);

        // Execute fill
        InputStream is = new ByteArrayInputStream(templateBytes);
        byte[] resultBytes = filler.fillMultiSheetTemplate(is, sheetDataList, "sheetName");

        // Verify result
        Workbook resultWorkbook = new XSSFWorkbook(new ByteArrayInputStream(resultBytes));
        assertEquals(2, resultWorkbook.getNumberOfSheets());
        
        assertEquals("SheetA", resultWorkbook.getSheetName(0));
        assertEquals("Hello Alpha", resultWorkbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
        
        assertEquals("SheetB", resultWorkbook.getSheetName(1));
        assertEquals("Hello Beta", resultWorkbook.getSheetAt(1).getRow(0).getCell(0).getStringCellValue());

        resultWorkbook.close();
    }
}
