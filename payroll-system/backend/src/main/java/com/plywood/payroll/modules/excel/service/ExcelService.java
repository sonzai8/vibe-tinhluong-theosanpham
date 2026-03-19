package com.plywood.payroll.modules.excel.service;

import com.plywood.payroll.modules.attendance.dto.request.DailyAttendanceRequest;
import com.plywood.payroll.modules.attendance.dto.response.DailyAttendanceResponse;
import com.plywood.payroll.modules.employee.dto.request.EmployeeRequest;
import com.plywood.payroll.modules.employee.repository.EmployeeRepository;
import com.plywood.payroll.modules.organization.dto.request.DepartmentRequest;
import com.plywood.payroll.modules.organization.dto.request.RoleRequest;
import com.plywood.payroll.modules.organization.dto.request.TeamRequest;
import com.plywood.payroll.modules.organization.repository.DepartmentRepository;
import com.plywood.payroll.modules.organization.repository.RoleRepository;
import com.plywood.payroll.modules.organization.repository.TeamRepository;
import com.plywood.payroll.modules.pricing.repository.ProductStepRateRepository;
import com.plywood.payroll.modules.pricing.entity.ProductStepRate;
import com.plywood.payroll.modules.product.dto.request.ProductRequest;
import com.plywood.payroll.modules.production.dto.request.ProductionStepRequest;
import com.plywood.payroll.modules.pricing.dto.response.ProductStepRateResponse;
import com.plywood.payroll.modules.product.dto.response.ProductResponse;
import com.plywood.payroll.modules.production.dto.response.ProductionRecordResponse;
import com.plywood.payroll.modules.production.dto.response.ProductionStepResponse;
import com.plywood.payroll.modules.quality.dto.response.ProductQualityResponse;
import com.plywood.payroll.modules.attendance.repository.AttendanceDefinitionRepository;
import com.plywood.payroll.shared.utils.ExcelHelper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.plywood.payroll.modules.payroll.dto.response.PayrollItemResponse;
import com.plywood.payroll.modules.payroll.dto.response.PayrollDailyDetailResponse;

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
    private final AttendanceDefinitionRepository attendanceDefinitionRepository;
    private final ProductStepRateRepository productStepRateRepository;

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
                row.createCell(5).setCellValue(att.getAttendanceDefinition() != null ? att.getAttendanceDefinition().getCode() : "");
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportAttendanceMatrix(List<DailyAttendanceResponse> attendances, List<LocalDate> dates) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Matrix");
            CellStyle headerStyle = ExcelHelper.createHeaderStyle(workbook);
            
            // Header: Code, Name, Team, Dates...
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Mã NV");
            headerRow.createCell(1).setCellValue("Họ Tên");
            headerRow.createCell(2).setCellValue("Tổ");
            
            for (int i = 0; i < dates.size(); i++) {
                Cell cell = headerRow.createCell(i + 3);
                cell.setCellValue(dates.get(i).getDayOfMonth());
                cell.setCellStyle(headerStyle);
            }
            
            // Group attendances by employee
            java.util.Map<Long, List<DailyAttendanceResponse>> employeeMap = attendances.stream()
                .collect(java.util.stream.Collectors.groupingBy(a -> a.getEmployee().getId()));
            
            // Get all employees from the data
            List<com.plywood.payroll.modules.employee.dto.response.EmployeeResponse> employees = attendances.stream()
                .map(DailyAttendanceResponse::getEmployee)
                .distinct()
                .sorted(java.util.Comparator.comparing(e -> e.getTeam() != null ? e.getTeam().getName() : ""))
                .collect(java.util.stream.Collectors.toList());
            
            int rowIdx = 1;
            for (com.plywood.payroll.modules.employee.dto.response.EmployeeResponse emp : employees) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(emp.getCode());
                row.createCell(1).setCellValue(emp.getFullName());
                row.createCell(2).setCellValue(emp.getTeam() != null ? emp.getTeam().getName() : "");
                
                List<DailyAttendanceResponse> empAtts = employeeMap.getOrDefault(emp.getId(), new ArrayList<>());
                for (int i = 0; i < dates.size(); i++) {
                    LocalDate date = dates.get(i);
                    DailyAttendanceResponse att = empAtts.stream()
                        .filter(a -> a.getAttendanceDate().equals(date))
                        .findFirst().orElse(null);
                    
                    if (att != null) {
                        row.createCell(i + 3).setCellValue(att.getAttendanceDefinition() != null ? att.getAttendanceDefinition().getCode() : "X");
                    }
                }
            }
            
            for (int i = 0; i < dates.size() + 3; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] getImportTemplate() throws IOException {
        try {
            return getTemplateBytes(ATTENDANCE_IMPORT_TEMPLATE);
        } catch (IOException e) {
            return generateDynamicAttendanceTemplate();
        }
    }

    public byte[] getEmployeeTemplate() throws IOException {
        try {
            return getTemplateBytes(EMPLOYEE_TEMPLATE);
        } catch (IOException e) {
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

            String[] depts = departmentRepository.findAll().stream()
                    .map(com.plywood.payroll.modules.organization.entity.Department::getName)
                    .toArray(String[]::new);
            ExcelHelper.addDataValidation(sheet, depts, 1, 1000, 4, 4);

            String[] teams = teamRepository.findAll().stream()
                    .map(com.plywood.payroll.modules.organization.entity.Team::getName)
                    .toArray(String[]::new);
            ExcelHelper.addDataValidation(sheet, teams, 1, 1000, 5, 5);

            String[] roles = roleRepository.findAll().stream()
                    .map(com.plywood.payroll.modules.organization.entity.Role::getName)
                    .toArray(String[]::new);
            ExcelHelper.addDataValidation(sheet, roles, 1, 1000, 6, 6);

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

                    String defCode = ExcelHelper.getCellValueAsString(currentRow.getCell(5));
                    if (defCode != null && !defCode.isEmpty()) {
                        attendanceDefinitionRepository.findByCode(defCode).ifPresent(def -> req.setAttendanceDefinitionId(def.getId()));
                    }

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

    public List<TeamRequest> importTeams(MultipartFile file) throws IOException {
        List<TeamRequest> requests = new ArrayList<>();
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
                    TeamRequest req = new TeamRequest();
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

    public byte[] exportProductStepRatesList(List<ProductStepRateResponse> rates) throws IOException {
        String[] headers = {"Sản phẩm", "Công đoạn", "Chất lượng", "Giá High", "Giá Low", "Ngày hiệu lực"};
        List<List<String>> data = new ArrayList<>();
        for (ProductStepRateResponse r : rates) {
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
            List<ProductResponse> products,
            List<ProductionStepResponse> steps,
            List<ProductQualityResponse> qualities,
            List<ProductStepRateResponse> rates) throws IOException {

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Ma trận đơn giá");
            CellStyle headerStyle = ExcelHelper.createHeaderStyle(workbook);
            CellStyle subHeaderStyle = workbook.createCellStyle();
            subHeaderStyle.cloneStyleFrom(headerStyle);
            subHeaderStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());

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

            int rowIdx = 1;
            for (ProductResponse p : products) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(p.getCode() + " (" + p.getThickness() + "x" + p.getLength() + "x" + p.getWidth() + ")");

                for (int sIdx = 0; sIdx < steps.size(); sIdx++) {
                    Long stepId = steps.get(sIdx).getId();
                    StringBuilder cellContent = new StringBuilder();

                    for (ProductQualityResponse q : qualities) {
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

    private byte[] generateDynamicAttendanceTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Điểm danh");
            Row header = sheet.createRow(0);
            String[] headers = {"Mã NV", "Họ Tên", "Ngày (YYYY-MM-DD)", "Tổ biên chế", "Tổ thực tế", "Mã loại công"};

            CellStyle headerStyle = ExcelHelper.createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }

            // Thêm hướng dẫn ở dòng 1
            Row hintRow = sheet.createRow(1);
            hintRow.createCell(5).setCellValue("VD: NG (Ca ngày), D (Ca đêm), P (Có phép)");

            // Data validation cho Mã loại công
            String[] defCodes = attendanceDefinitionRepository.findAll().stream()
                    .map(com.plywood.payroll.modules.attendance.entity.AttendanceDefinition::getCode)
                    .toArray(String[]::new);
            
            if (defCodes.length > 0) {
                ExcelHelper.addDataValidation(sheet, defCodes, 1, 1000, 5, 5);
            }

            return ExcelHelper.createWorkbookBytes(workbook);
        }
    }

    public byte[] exportProductionRecords(List<ProductionRecordResponse> records) throws IOException {
        String[] headers = {"Ngày", "Tổ", "Công đoạn", "Sản phẩm", "Chất lượng", "Số lượng"};
        List<List<String>> data = new ArrayList<>();
        for (ProductionRecordResponse r : records) {
            List<String> row = new ArrayList<>();
            row.add(r.getProductionDate().toString());
            row.add(r.getTeam() != null ? r.getTeam().getName() : "");
            row.add(r.getTeam() != null && r.getTeam().getProductionStep() != null ? r.getTeam().getProductionStep().getName() : "");
            row.add(r.getProduct() != null ? r.getProduct().getCode() : "");
            row.add(r.getQuality() != null ? r.getQuality().getCode() : "");
            row.add(r.getQuantity() != null ? r.getQuantity().toString() : "0");
            data.add(row);
        }
        return exportGeneric("Danh sách sản lượng", headers, data);
    }

    public byte[] exportProductionRecordMatrix(List<ProductionRecordResponse> records, List<LocalDate> dates) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // Nhóm dữ liệu theo tổ
            java.util.Map<Long, List<ProductionRecordResponse>> teamMap = records.stream()
                .filter(r -> r.getTeam() != null)
                .collect(java.util.stream.Collectors.groupingBy(r -> r.getTeam().getId()));
            
            if (teamMap.isEmpty()) {
                Sheet sheet = workbook.createSheet("Trống");
                sheet.createRow(0).createCell(0).setCellValue("Không có dữ liệu trong khoảng thời gian này.");
            } else {
                for (java.util.Map.Entry<Long, List<ProductionRecordResponse>> entry : teamMap.entrySet()) {
                    List<ProductionRecordResponse> teamRecords = entry.getValue();
                    com.plywood.payroll.modules.organization.dto.response.TeamResponse team = teamRecords.get(0).getTeam();
                    
                    String safeSheetName = team.getName().replaceAll("[\\\\/:*?\"\\[\\]]", "_");
                    Sheet sheet = workbook.createSheet(safeSheetName);
                    buildDetailedTeamSheet(sheet, teamRecords, dates, workbook);
                }
            }
            
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void buildDetailedTeamSheet(Sheet sheet, List<ProductionRecordResponse> teamRecords, List<LocalDate> dates, Workbook workbook) {
        CellStyle headerStyle = ExcelHelper.createHeaderStyle(workbook);
        CellStyle centerStyle = workbook.createCellStyle();
        centerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        centerStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        centerStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        centerStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        centerStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        
        CellStyle priceStyle = workbook.createCellStyle();
        priceStyle.cloneStyleFrom(centerStyle);
        priceStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LEMON_CHIFFON.getIndex());
        priceStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        
        // 1. Phân tích cột: Lấy danh sách duy nhất Product-Quality và phân nhóm
        List<ColumnMeta> distinctMetas = teamRecords.stream()
            .map(r -> new ColumnMeta(r.getProduct(), r.getQuality()))
            .distinct()
            .collect(java.util.stream.Collectors.toList());

        java.util.Map<String, List<ColumnMeta>> columnGroups = distinctMetas.stream()
            .collect(java.util.stream.Collectors.groupingBy(m -> {
                if (m.getProduct() == null || m.getProduct().getFilmCoatingType() == null) return "Nhật ván";
                return switch (m.getProduct().getFilmCoatingType()) {
                    case SIDE_1 -> "1M";
                    case SIDE_2 -> "2M";
                    default -> "Nhật ván";
                };
            }));

        List<String> sortedGroupKeys = new ArrayList<>(columnGroups.keySet());
        sortedGroupKeys.sort(java.util.Comparator.comparing(k -> {
            if ("1M".equals(k)) return 1;
            if ("2M".equals(k)) return 2;
            return 3;
        }));

        List<ColumnMeta> allColumns = new ArrayList<>();
        for (String groupKey : sortedGroupKeys) {
            List<ColumnMeta> groupCols = columnGroups.get(groupKey).stream()
                .sorted(java.util.Comparator.comparing(c -> c.getProduct().getCode()))
                .collect(java.util.stream.Collectors.toList());
            
            // Gán group name cho từng cột để làm header
            groupCols.forEach(c -> c.setGroupName(groupKey));
            allColumns.addAll(groupCols);
        }

        // Headers
        Row groupHeaderRow = sheet.createRow(0);
        Row subHeaderRow = sheet.createRow(1);
        Row dgMoiRow = sheet.createRow(2);
        Row dgCuRow = sheet.createRow(3);

        groupHeaderRow.createCell(0).setCellValue("Ngày");
        groupHeaderRow.getCell(0).setCellStyle(headerStyle);
        subHeaderRow.createCell(0); // Empty
        dgMoiRow.createCell(0).setCellValue("DG mới");
        dgMoiRow.getCell(0).setCellStyle(priceStyle);
        dgCuRow.createCell(0).setCellValue("DG cũ");
        dgCuRow.getCell(0).setCellStyle(priceStyle);

        int colIdx = 1;
        int lastGroupStart = 1;
        String currentGroup = null;

        for (int i = 0; i < allColumns.size(); i++) {
            ColumnMeta col = allColumns.get(i);
            
            // Group header merging
            if (currentGroup == null || !currentGroup.equals(col.getGroupName())) {
                if (currentGroup != null && colIdx - 1 > lastGroupStart) {
                    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, lastGroupStart, colIdx - 1));
                }
                currentGroup = col.getGroupName();
                lastGroupStart = colIdx;
                Cell groupCell = groupHeaderRow.createCell(colIdx);
                groupCell.setCellValue(currentGroup);
                groupCell.setCellStyle(headerStyle);
            } else {
                groupHeaderRow.createCell(colIdx).setCellStyle(headerStyle);
            }

            // Sub header: Code, Thickness, Quality
            String subHead = String.format("%s-%s", col.getProduct().getThickness().stripTrailingZeros().toPlainString(), col.getProduct().getCode());
            if (col.getQuality() != null) subHead += " (" + col.getQuality().getCode() + ")";
            Cell subCell = subHeaderRow.createCell(colIdx);
            subCell.setCellValue(subHead);
            subCell.setCellStyle(centerStyle);

            // Fetch Prices
            Long stepId = teamRecords.get(0).getTeam().getProductionStep().getId();
            java.util.Optional<ProductStepRate> rateOpt = productStepRateRepository.findEffectiveRate(
                col.getProduct().getId(), stepId, col.getQuality().getId(), dates.get(dates.size()-1));
            
            Cell cellDgMoi = dgMoiRow.createCell(colIdx);
            Cell cellDgCu = dgCuRow.createCell(colIdx);
            cellDgMoi.setCellStyle(priceStyle);
            cellDgCu.setCellStyle(priceStyle);

            if (rateOpt.isPresent()) {
                cellDgMoi.setCellValue(rateOpt.get().getPriceHigh().doubleValue());
                cellDgCu.setCellValue(rateOpt.get().getPriceLow().doubleValue());
                col.setPriceHigh(rateOpt.get().getPriceHigh());
                col.setPriceLow(rateOpt.get().getPriceLow());
            }

            colIdx++;
        }
        // Last group merge
        if (allColumns.size() > 0 && colIdx - 1 > lastGroupStart) {
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, lastGroupStart, colIdx - 1));
        }

        // Extra Columns: Tổ Khác, Đầu Chuyền, Tổng Tiền, Tổng Tiền ĐG mới
        String[] extras = {"Tổ Khác", "Đầu Chuyền", "Tổng Tiền", "Tổng tiền ĐG mới"};
        for (String extra : extras) {
            Cell cell = groupHeaderRow.createCell(colIdx);
            cell.setCellValue(extra);
            cell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 1, colIdx, colIdx));
            dgMoiRow.createCell(colIdx).setCellStyle(priceStyle);
            dgCuRow.createCell(colIdx).setCellStyle(priceStyle);
            colIdx++;
        }

        // Data Rows
        int rowIdx = 4;
        for (LocalDate date : dates) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(date.getDayOfMonth());
            row.getCell(0).setCellStyle(centerStyle);

            java.math.BigDecimal dayTotalMoi = java.math.BigDecimal.ZERO;
            java.math.BigDecimal dayTotalCu = java.math.BigDecimal.ZERO;
            boolean hasProduction = false;

            for (int i = 0; i < allColumns.size(); i++) {
                ColumnMeta col = allColumns.get(i);
                int qty = teamRecords.stream()
                    .filter(r -> r.getProductionDate().equals(date) 
                             && r.getProduct().getId().equals(col.getProduct().getId())
                             && r.getQuality().getId().equals(col.getQuality().getId()))
                    .mapToInt(r -> r.getQuantity() != null ? r.getQuantity() : 0)
                    .sum();
                
                Cell dataCell = row.createCell(i + 1);
                dataCell.setCellStyle(centerStyle);
                if (qty > 0) {
                    hasProduction = true;
                    dataCell.setCellValue(qty);
                    if (col.getPriceHigh() != null) {
                        dayTotalMoi = dayTotalMoi.add(col.getPriceHigh().multiply(java.math.BigDecimal.valueOf(qty)));
                    }
                    if (col.getPriceLow() != null) {
                        dayTotalCu = dayTotalCu.add(col.getPriceLow().multiply(java.math.BigDecimal.valueOf(qty)));
                    }
                }
            }

            // Calculations for extra columns
            int baseColIdx = allColumns.size() + 1;
            Cell cellToKhac = row.createCell(baseColIdx);
            cellToKhac.setCellStyle(centerStyle);
            
            Cell cellDauChuyen = row.createCell(baseColIdx + 1);
            cellDauChuyen.setCellStyle(centerStyle);

            Cell cellTotalCu = row.createCell(baseColIdx + 2);
            cellTotalCu.setCellStyle(centerStyle);

            Cell cellTotalMoi = row.createCell(baseColIdx + 3);
            cellTotalMoi.setCellStyle(centerStyle);

            if (hasProduction) {
                cellToKhac.setCellValue(0);
                cellDauChuyen.setCellValue(20000);
                cellTotalCu.setCellValue(dayTotalCu.doubleValue() + 20000);
                cellTotalMoi.setCellValue(dayTotalMoi.doubleValue() + 20000);
            } else {
                cellToKhac.setCellValue("-");
                cellDauChuyen.setCellValue("-");
                cellTotalCu.setCellValue("-");
                cellTotalMoi.setCellValue("-");
            }
        }

        // Finalize: Auto Size
        for (int i = 0; i < colIdx; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    @lombok.Data
    private static class ColumnMeta {
        private final com.plywood.payroll.modules.product.dto.response.ProductResponse product;
        private final com.plywood.payroll.modules.quality.dto.response.ProductQualityResponse quality;
        private String groupName;
        private java.math.BigDecimal priceHigh;
        private java.math.BigDecimal priceLow;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ColumnMeta that = (ColumnMeta) o;
            return java.util.Objects.equals(product.getId(), that.product.getId()) &&
                   java.util.Objects.equals(quality.getId(), that.quality.getId());
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(product.getId(), quality.getId());
        }
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        return row.getPhysicalNumberOfCells() == 0;
    }

    public byte[] exportPayslips(int month, int year, List<PayrollItemResponse> items, java.util.Map<Long, List<PayrollDailyDetailResponse>> detailsMap) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
             
            CellStyle headerStyle = ExcelHelper.createHeaderStyle(workbook);
            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);

            CellStyle boldStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);
            
            CellStyle numberStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.DataFormat format = workbook.createDataFormat();
            numberStyle.setDataFormat(format.getFormat("#,##0"));

            CellStyle boldNumberStyle = workbook.createCellStyle();
            boldNumberStyle.setFont(boldFont);
            boldNumberStyle.setDataFormat(format.getFormat("#,##0"));

            for (PayrollItemResponse item : items) {
                String safeName = item.getEmployeeName().replaceAll("[\\\\/:*?\"\\[\\]]", "_");
                if (safeName.length() > 31) {
                    safeName = safeName.substring(0, 31);
                }
                // Determine valid sheet name (might have duplicates if same name, handle by adding id)
                String sheetName = safeName;
                int suffix = 1;
                while (workbook.getSheet(sheetName) != null) {
                    sheetName = safeName + "_" + suffix;
                    if (sheetName.length() > 31) {
                        sheetName = safeName.substring(0, 31 - String.valueOf(suffix).length() - 1) + "_" + suffix;
                    }
                    suffix++;
                }

                Sheet sheet = workbook.createSheet(sheetName);

                // --- Header Section ---
                Row titleRow = sheet.createRow(0);
                Cell titleCell = titleRow.createCell(0);
                titleCell.setCellValue("PHIẾU LƯƠNG THÁNG " + month + "/" + year);
                titleCell.setCellStyle(titleStyle);
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

                int r = 2;
                
                // Employee Info
                Row empRow1 = sheet.createRow(r++);
                empRow1.createCell(0).setCellValue("Họ và tên:");
                empRow1.getCell(0).setCellStyle(boldStyle);
                empRow1.createCell(1).setCellValue(item.getEmployeeName());
                
                empRow1.createCell(2).setCellValue("Mã NV:");
                empRow1.getCell(2).setCellStyle(boldStyle);
                empRow1.createCell(3).setCellValue(item.getEmployeeCode());

                Row empRow2 = sheet.createRow(r++);
                empRow2.createCell(0).setCellValue("Phòng ban:");
                empRow2.getCell(0).setCellStyle(boldStyle);
                empRow2.createCell(1).setCellValue(item.getDepartmentName() != null ? item.getDepartmentName() : "");
                
                empRow2.createCell(2).setCellValue("Tổ:");
                empRow2.getCell(2).setCellStyle(boldStyle);
                empRow2.createCell(3).setCellValue(item.getTeamName() != null ? item.getTeamName() : "");

                r++; // blank row

                // --- Salary Summary ---
                Row summaryTitleRow = sheet.createRow(r++);
                summaryTitleRow.createCell(0).setCellValue("TỔNG HỢP LƯƠNG");
                summaryTitleRow.getCell(0).setCellStyle(boldStyle);
                sheet.addMergedRegion(new CellRangeAddress(r-1, r-1, 0, 3));

                Row sumRow1 = sheet.createRow(r++);
                sumRow1.createCell(0).setCellValue("Lương sản phẩm (1):");
                Cell c1 = sumRow1.createCell(1);
                c1.setCellValue(item.getProductSalary() != null ? item.getProductSalary().doubleValue() : 0);
                c1.setCellStyle(numberStyle);

                Row sumRow2 = sheet.createRow(r++);
                sumRow2.createCell(0).setCellValue("Phụ cấp chức vụ (2):");
                Cell c2 = sumRow2.createCell(1);
                c2.setCellValue(item.getBenefitSalary() != null ? item.getBenefitSalary().doubleValue() : 0);
                c2.setCellStyle(numberStyle);

                Row sumRow3 = sheet.createRow(r++);
                sumRow3.createCell(0).setCellValue("Thưởng / Phạt (3):");
                Cell c3 = sumRow3.createCell(1);
                c3.setCellValue(item.getTotalPenaltyBonus() != null ? item.getTotalPenaltyBonus().doubleValue() : 0);
                c3.setCellStyle(numberStyle);

                r++;
                Row netRow = sheet.createRow(r++);
                netRow.createCell(0).setCellValue("THỰC LĨNH = (1) + (2) + (3):");
                netRow.getCell(0).setCellStyle(boldStyle);
                Cell cNet = netRow.createCell(1);
                cNet.setCellValue(item.getTotalSalary() != null ? item.getTotalSalary().doubleValue() : 0);
                cNet.setCellStyle(boldNumberStyle);

                r += 2; // blank row

                // --- Daily Details ---
                List<PayrollDailyDetailResponse> details = detailsMap.get(item.getId());
                if (details != null && !details.isEmpty()) {
                    Row detailTitleRow = sheet.createRow(r++);
                    detailTitleRow.createCell(0).setCellValue("CHI TIẾT LƯƠNG TỪNG NGÀY");
                    detailTitleRow.getCell(0).setCellStyle(boldStyle);
                    sheet.addMergedRegion(new CellRangeAddress(r-1, r-1, 0, 5));

                    Row thRow = sheet.createRow(r++);
                    String[] headers = {"Ngày", "Chấm công", "Tổ làm việc", "Lương SP (đ)", "Phụ cấp (đ)", "Thưởng/Phạt (đ)"};
                    for (int i = 0; i < headers.length; i++) {
                        Cell c = thRow.createCell(i);
                        c.setCellValue(headers[i]);
                        c.setCellStyle(headerStyle);
                    }

                    for (PayrollDailyDetailResponse d : details) {
                        Row dr = sheet.createRow(r++);
                        
                        dr.createCell(0).setCellValue(d.getDate() != null ? d.getDate().toString() : "");
                        dr.createCell(1).setCellValue(d.getAttendanceSymbol() != null ? d.getAttendanceSymbol() : "");
                        dr.createCell(2).setCellValue(d.getTeamName() != null ? d.getTeamName() : "");
                        
                        Cell spC = dr.createCell(3);
                        spC.setCellValue(d.getProductSalary() != null ? d.getProductSalary().doubleValue() : 0);
                        spC.setCellStyle(numberStyle);

                        Cell pcC = dr.createCell(4);
                        pcC.setCellValue(d.getBenefitSalary() != null ? d.getBenefitSalary().doubleValue() : 0);
                        pcC.setCellStyle(numberStyle);

                        double tBonus = d.getBonus() != null ? d.getBonus().doubleValue() : 0;
                        double tPenalty = d.getPenalty() != null ? d.getPenalty().doubleValue() : 0;
                        
                        Cell penC = dr.createCell(5);
                        penC.setCellValue(tBonus - tPenalty);
                        penC.setCellStyle(numberStyle);
                    }
                }

                // Auto size column
                for (int i = 0; i < 6; i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
