package com.plywood.payroll.service;

import com.plywood.payroll.dto.request.*;
import com.plywood.payroll.dto.response.DailyAttendanceResponse;
import com.plywood.payroll.repository.*;
import com.plywood.payroll.utils.ExcelHelper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;

    private static final String ATTENDANCE_IMPORT_TEMPLATE = "templates/attendance/import/import_template.xlsx";
    private static final String ATTENDANCE_EXPORT_TEMPLATE = "templates/attendance/export/export_template.xlsx";
    private static final String EMPLOYEE_TEMPLATE = "templates/employee/employee_template.xlsx";

    public byte[] exportAttendances(List<DailyAttendanceResponse> attendances) throws IOException {
        ClassPathResource resource = new ClassPathResource(ATTENDANCE_EXPORT_TEMPLATE);
        try (InputStream is = resource.getInputStream();
             Workbook workbook = ExcelHelper.createWorkbook(is);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.getSheetAt(0);
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

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] getImportTemplate() throws IOException {
        return getTemplateBytes(ATTENDANCE_IMPORT_TEMPLATE);
    }

    public byte[] getEmployeeTemplate() throws IOException {
        // Nếu template file chưa tồn tại, ta có thể generate động hoặc trả về file trắng có header
        try {
            return getTemplateBytes(EMPLOYEE_TEMPLATE);
        } catch (IOException e) {
            // Fallback: Generate template dynamically if file missing
            return generateDynamicEmployeeTemplate();
        }
    }

    private byte[] getTemplateBytes(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        try (InputStream is = resource.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            is.transferTo(out);
            return out.toByteArray();
        }
    }

    private byte[] generateDynamicEmployeeTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Nhân viên");
            Row header = sheet.createRow(0);
            String[] headers = {"Mã NV", "Họ Tên", "Tên đăng nhập", "Mật khẩu", "Phòng ban", "Tổ đội", "Chức vụ", "Quyền Đăng nhập (Y/N)"};
            
            CellStyle headerStyle = ExcelHelper.createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }

            // Dropdown list for Departments (column 4 - "Phòng ban")
            String[] depts = departmentRepository.findAll().stream()
                    .map(com.plywood.payroll.entity.Department::getName)
                    .toArray(String[]::new);
            ExcelHelper.addDataValidation(sheet, depts, 1, 1000, 4, 4);

            // Dropdown list for Teams (column 5 - "Tổ đội")
            String[] teams = teamRepository.findAll().stream()
                    .map(com.plywood.payroll.entity.Team::getName)
                    .toArray(String[]::new);
            ExcelHelper.addDataValidation(sheet, teams, 1, 1000, 5, 5);

            // Dropdown list for Roles (column 6 - "Chức vụ")
            String[] roles = roleRepository.findAll().stream()
                    .map(com.plywood.payroll.entity.Role::getName)
                    .toArray(String[]::new);
            ExcelHelper.addDataValidation(sheet, roles, 1, 1000, 6, 6);

            // Dropdown for Access (column 7 - "Quyền Đăng nhập")
            ExcelHelper.addDataValidation(sheet, new String[]{"Y", "N", "Có", "Không"}, 1, 1000, 7, 7);
            
            return ExcelHelper.createWorkbookBytes(workbook);
        }
    }

    public List<DailyAttendanceRequest> importAttendances(MultipartFile file) throws IOException {
        List<DailyAttendanceRequest> requests = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook workbook = ExcelHelper.createWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            if (rows.hasNext()) rows.next(); // Skip header

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                if (isRowEmpty(currentRow)) continue;

                String empCode = ExcelHelper.getCellValueAsString(currentRow.getCell(0));
                if (empCode == null || empCode.isEmpty()) continue;

                employeeRepository.findByCode(empCode).ifPresent(emp -> {
                    DailyAttendanceRequest req = new DailyAttendanceRequest();
                    req.setEmployeeId(emp.getId());
                    
                    String dateStr = ExcelHelper.getCellValueAsString(currentRow.getCell(2));
                    req.setAttendanceDate(dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now());

                    String origTeamName = ExcelHelper.getCellValueAsString(currentRow.getCell(3));
                    if (origTeamName != null) teamRepository.findByName(origTeamName).ifPresent(t -> req.setOriginalTeamId(t.getId()));
                    
                    String actualTeamName = ExcelHelper.getCellValueAsString(currentRow.getCell(4));
                    if (actualTeamName != null) teamRepository.findByName(actualTeamName).ifPresent(t -> req.setActualTeamId(t.getId()));
                    else req.setActualTeamId(req.getOriginalTeamId());

                    requests.add(req);
                });
            }
        }
        return requests;
    }

    public List<EmployeeRequest> importEmployees(MultipartFile file) throws IOException {
        List<EmployeeRequest> requests = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook workbook = ExcelHelper.createWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            if (rows.hasNext()) rows.next(); // Skip header

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                if (isRowEmpty(currentRow)) continue;

                String code = ExcelHelper.getCellValueAsString(currentRow.getCell(0));
                String name = ExcelHelper.getCellValueAsString(currentRow.getCell(1));
                if (code == null || code.isEmpty() || name == null || name.isEmpty()) continue;

                EmployeeRequest req = new EmployeeRequest();
                req.setCode(code);
                req.setFullName(name);
                req.setUsername(ExcelHelper.getCellValueAsString(currentRow.getCell(2)));
                req.setPassword(ExcelHelper.getCellValueAsString(currentRow.getCell(3)));
                
                String deptName = ExcelHelper.getCellValueAsString(currentRow.getCell(4));
                if (deptName != null) departmentRepository.findByName(deptName).ifPresent(d -> req.setDepartmentId(d.getId()));

                String teamName = ExcelHelper.getCellValueAsString(currentRow.getCell(5));
                if (teamName != null) teamRepository.findByName(teamName).ifPresent(t -> req.setTeamId(t.getId()));

                String roleName = ExcelHelper.getCellValueAsString(currentRow.getCell(6));
                if (roleName != null) roleRepository.findByName(roleName).ifPresent(r -> req.setRoleId(r.getId()));

                String canLogin = ExcelHelper.getCellValueAsString(currentRow.getCell(7));
                req.setCanLogin("Y".equalsIgnoreCase(canLogin) || "Có".equalsIgnoreCase(canLogin) || "1".equals(canLogin));

                requests.add(req);
            }
        }
        return requests;
    }

    // --- UNIVERSAL METHODS ---

    public byte[] exportGeneric(String sheetName, String[] headers, List<List<String>> data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = ExcelHelper.createHeaderStyle(workbook);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }

            int rowIdx = 1;
            for (List<String> rowData : data) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < rowData.size(); i++) {
                    row.createCell(i).setCellValue(rowData.get(i));
                }
            }

            return ExcelHelper.createWorkbookBytes(workbook);
        }
    }

    public byte[] getGenericTemplate(String sheetName, String[] headers) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = ExcelHelper.createHeaderStyle(workbook);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }
            return ExcelHelper.createWorkbookBytes(workbook);
        }
    }

    public List<DepartmentRequest> importDepartments(MultipartFile file) throws IOException {
        List<DepartmentRequest> requests = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook workbook = ExcelHelper.createWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            if (rows.hasNext()) rows.next();
            while (rows.hasNext()) {
                Row row = rows.next();
                if (isRowEmpty(row)) continue;
                String name = ExcelHelper.getCellValueAsString(row.getCell(0));
                if (name != null && !name.isEmpty()) {
                    DepartmentRequest req = new DepartmentRequest();
                    req.setName(name);
                    requests.add(req);
                }
            }
        }
        return requests;
    }

    public List<RoleRequest> importRoles(MultipartFile file) throws IOException {
        List<RoleRequest> requests = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook workbook = ExcelHelper.createWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            if (rows.hasNext()) rows.next();
            while (rows.hasNext()) {
                Row row = rows.next();
                if (isRowEmpty(row)) continue;
                String name = ExcelHelper.getCellValueAsString(row.getCell(0));
                String benefit = ExcelHelper.getCellValueAsString(row.getCell(1));
                if (name != null && !name.isEmpty()) {
                    RoleRequest req = new RoleRequest();
                    req.setName(name);
                    req.setDailyBenefit(benefit != null ? new java.math.BigDecimal(benefit) : java.math.BigDecimal.ZERO);
                    requests.add(req);
                }
            }
        }
        return requests;
    }

    public List<com.plywood.payroll.dto.request.TeamRequest> importTeams(MultipartFile file) throws IOException {
        List<com.plywood.payroll.dto.request.TeamRequest> requests = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook workbook = ExcelHelper.createWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            if (rows.hasNext()) rows.next();
            while (rows.hasNext()) {
                Row row = rows.next();
                if (isRowEmpty(row)) continue;
                String name = ExcelHelper.getCellValueAsString(row.getCell(0));
                String deptName = ExcelHelper.getCellValueAsString(row.getCell(1));
                if (name != null && !name.isEmpty()) {
                    com.plywood.payroll.dto.request.TeamRequest req = new com.plywood.payroll.dto.request.TeamRequest();
                    req.setName(name);
                    if (deptName != null) {
                        departmentRepository.findByName(deptName).ifPresent(d -> req.setDepartmentId(d.getId()));
                    }
                    requests.add(req);
                }
            }
        }
        return requests;
    }

    public List<ProductRequest> importProducts(MultipartFile file) throws IOException {
        List<ProductRequest> requests = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook workbook = ExcelHelper.createWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            if (rows.hasNext()) rows.next();
            while (rows.hasNext()) {
                Row row = rows.next();
                if (isRowEmpty(row)) continue;
                String code = ExcelHelper.getCellValueAsString(row.getCell(0));
                String thickness = ExcelHelper.getCellValueAsString(row.getCell(1));
                String length = ExcelHelper.getCellValueAsString(row.getCell(2));
                String width = ExcelHelper.getCellValueAsString(row.getCell(3));
                
                if (code != null && !code.isEmpty()) {
                    ProductRequest req = new ProductRequest();
                    req.setCode(code);
                    req.setThickness(thickness != null ? new java.math.BigDecimal(thickness) : java.math.BigDecimal.ZERO);
                    req.setLength(length != null ? new java.math.BigDecimal(length) : java.math.BigDecimal.ZERO);
                    req.setWidth(width != null ? new java.math.BigDecimal(width) : java.math.BigDecimal.ZERO);
                    requests.add(req);
                }
            }
        }
        return requests;
    }

    public List<ProductionStepRequest> importProductionSteps(MultipartFile file) throws IOException {
        List<ProductionStepRequest> requests = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook workbook = ExcelHelper.createWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            if (rows.hasNext()) rows.next();
            while (rows.hasNext()) {
                Row row = rows.next();
                if (isRowEmpty(row)) continue;
                String name = ExcelHelper.getCellValueAsString(row.getCell(0));
                String desc = ExcelHelper.getCellValueAsString(row.getCell(1));
                if (name != null && !name.isEmpty()) {
                    ProductionStepRequest req = new ProductionStepRequest();
                    req.setName(name);
                    req.setDescription(desc);
                    requests.add(req);
                }
            }
        }
        return requests;
    }

    // Specific mapping helpers can be added here or in the controllers calling generic methods

    public byte[] exportProductStepRatesList(List<com.plywood.payroll.dto.response.ProductStepRateResponse> rates) throws IOException {
        String[] headers = {"Sản phẩm", "Công đoạn", "Chất lượng", "Giá High", "Giá Low", "Ngày hiệu lực"};
        List<List<String>> data = new ArrayList<>();
        for (com.plywood.payroll.dto.response.ProductStepRateResponse r : rates) {
            List<String> row = new ArrayList<>();
            row.add(r.getProduct().getCode());
            row.add(r.getProductionStep().getName());
            row.add(r.getQuality().getCode());
            row.add(r.getPriceHigh().toString());
            row.add(r.getPriceLow().toString());
            row.add(r.getEffectiveDate().toString());
            data.add(row);
        }
        return exportGeneric("Danh sách đơn giá", headers, data);
    }

    public byte[] exportProductStepRatesMatrix(
            List<com.plywood.payroll.dto.response.ProductResponse> products,
            List<com.plywood.payroll.dto.response.ProductionStepResponse> steps,
            List<com.plywood.payroll.dto.response.ProductQualityResponse> qualities,
            List<com.plywood.payroll.dto.response.ProductStepRateResponse> rates) throws IOException {
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Ma trận đơn giá");
            CellStyle headerStyle = ExcelHelper.createHeaderStyle(workbook);
            CellStyle subHeaderStyle = workbook.createCellStyle();
            subHeaderStyle.cloneStyleFrom(headerStyle);
            subHeaderStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());

            // Header row 1: Products \ Steps
            Row headerRow = sheet.createRow(0);
            Cell corner = headerRow.createCell(0);
            corner.setCellValue("Sản phẩm \\ Công đoạn");
            corner.setCellStyle(headerStyle);
            sheet.setColumnWidth(0, 30 * 256);

            for (int i = 0; i < steps.size(); i++) {
                Cell cell = headerRow.createCell(i + 1);
                cell.setCellValue(steps.get(i).getName());
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i + 1, 25 * 256);
            }

            // Data rows
            int rowIdx = 1;
            for (com.plywood.payroll.dto.response.ProductResponse p : products) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(p.getCode() + " (" + p.getThickness() + "x" + p.getLength() + "x" + p.getWidth() + ")");
                
                for (int sIdx = 0; sIdx < steps.size(); sIdx++) {
                    Long stepId = steps.get(sIdx).getId();
                    StringBuilder cellContent = new StringBuilder();
                    
                    for (com.plywood.payroll.dto.response.ProductQualityResponse q : qualities) {
                        Long qualityId = q.getId();
                        rates.stream()
                            .filter(r -> r.getProduct().getId().equals(p.getId()) && 
                                         r.getProductionStep().getId().equals(stepId) &&
                                         r.getQuality().getId().equals(qualityId))
                            .findFirst()
                            .ifPresent(r -> {
                                if (cellContent.length() > 0) cellContent.append("\n");
                                cellContent.append(q.getCode()).append(": ").append(r.getPriceHigh()).append("/").append(r.getPriceLow());
                            });
                    }
                    
                    Cell cell = row.createCell(sIdx + 1);
                    cell.setCellValue(cellContent.toString());
                    cell.getCellStyle().setWrapText(true);
                }
                row.setHeightInPoints(Math.max(row.getHeightInPoints(), (qualities.size() * 15f)));
            }

            return ExcelHelper.createWorkbookBytes(workbook);
        }
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        Cell firstCell = row.getCell(0);
        return firstCell == null || firstCell.getCellType() == CellType.BLANK;
    }
}
