package com.plywood.payroll.service;

import com.plywood.payroll.dto.request.DailyAttendanceRequest;
import com.plywood.payroll.dto.response.DailyAttendanceResponse;
import com.plywood.payroll.entity.Employee;
import com.plywood.payroll.repository.EmployeeRepository;
import com.plywood.payroll.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelService {

    private final EmployeeRepository employeeRepository;
    private final TeamRepository teamRepository;

    private static final String IMPORT_TEMPLATE = "templates/attendance/import/import_template.xlsx";
    private static final String EXPORT_TEMPLATE = "templates/attendance/export/export_template.xlsx";

    public byte[] exportAttendances(List<DailyAttendanceResponse> attendances) throws IOException {
        ClassPathResource resource = new ClassPathResource(EXPORT_TEMPLATE);
        try (InputStream is = resource.getInputStream(); 
             Workbook workbook = new XSSFWorkbook(is); 
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            // Data Rows start from index 1 (Row 2 in Excel)
            int rowIdx = 1;
            for (DailyAttendanceResponse att : attendances) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) row = sheet.createRow(rowIdx);
                rowIdx++;

                row.createCell(0).setCellValue(att.getEmployee().getCode());
                row.createCell(1).setCellValue(att.getEmployee().getFullName());
                row.createCell(2).setCellValue(att.getAttendanceDate().toString());
                row.createCell(3).setCellValue(att.getOriginalTeam() != null ? att.getOriginalTeam().getName() : "");
                row.createCell(4).setCellValue(att.getActualTeam() != null ? att.getActualTeam().getName() : "");
            }

            // Auto-size columns (optional, template might already be sized)
            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] getImportTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource(IMPORT_TEMPLATE);
        try (InputStream is = resource.getInputStream(); 
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
            return out.toByteArray();
        }
    }

    public List<DailyAttendanceRequest> importAttendances(MultipartFile file) throws IOException {
        List<DailyAttendanceRequest> requests = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip header (Row 0)
            if (rows.hasNext()) rows.next();

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                
                // Validate if row is empty
                Cell firstCell = currentRow.getCell(0);
                if (firstCell == null || firstCell.getCellType() == CellType.BLANK) continue;

                String empCode = getCellValueAsString(firstCell);
                if (empCode == null || empCode.isEmpty()) continue;

                Employee emp = employeeRepository.findByCode(empCode)
                        .orElse(null);
                
                if (emp == null) continue;

                DailyAttendanceRequest req = new DailyAttendanceRequest();
                req.setEmployeeId(emp.getId());
                
                String dateStr = getCellValueAsString(currentRow.getCell(2));
                if (dateStr != null) {
                    try {
                        req.setAttendanceDate(LocalDate.parse(dateStr));
                    } catch (Exception e) {
                        req.setAttendanceDate(LocalDate.now());
                    }
                } else {
                    req.setAttendanceDate(LocalDate.now());
                }

                // Import Team by Name if provided
                String origTeamName = getCellValueAsString(currentRow.getCell(3));
                if (origTeamName != null && !origTeamName.isEmpty()) {
                    teamRepository.findByName(origTeamName).ifPresent(t -> req.setOriginalTeamId(t.getId()));
                }
                
                String actualTeamName = getCellValueAsString(currentRow.getCell(4));
                if (actualTeamName != null && !actualTeamName.isEmpty()) {
                    teamRepository.findByName(actualTeamName).ifPresent(t -> req.setActualTeamId(t.getId()));
                } else {
                    req.setActualTeamId(req.getOriginalTeamId());
                }

                requests.add(req);
            }
        }
        return requests;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf((int)cell.getNumericCellValue());
            default: return null;
        }
    }
}
