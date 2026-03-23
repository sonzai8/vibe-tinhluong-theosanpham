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
import com.plywood.payroll.modules.product.dto.request.ProductRequest;
import com.plywood.payroll.modules.production.dto.request.ProductionStepRequest;
import com.plywood.payroll.modules.pricing.dto.response.ProductStepRateResponse;
import com.plywood.payroll.modules.product.dto.response.ProductResponse;
import com.plywood.payroll.modules.production.dto.response.ProductionRecordResponse;
import com.plywood.payroll.modules.quality.dto.response.ProductQualityResponse;
import com.plywood.payroll.modules.production.dto.response.ProductionStepResponse;
import com.plywood.payroll.modules.attendance.repository.AttendanceDefinitionRepository;
import com.plywood.payroll.modules.product.repository.ProductUnitRepository;
import com.plywood.payroll.modules.product.enums.FilmCoatingType;
import com.plywood.payroll.shared.utils.ExcelHelper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.plywood.payroll.modules.excel.dto.response.ImportError;
import com.plywood.payroll.modules.excel.dto.response.ImportResult;
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
    private final ProductUnitRepository productUnitRepository;
    private final com.plywood.payroll.shared.utils.ExcelTemplateFiller excelTemplateFiller;

    private static final String ATTENDANCE_IMPORT_TEMPLATE = "templates/attendance/import/import_template.xlsx";
    private static final String EMPLOYEE_TEMPLATE = "templates/import/employee_template.xlsx";
    private static final String PRODUCT_IMPORT_TEMPLATE = "templates/import/product_template.xlsx";

    public byte[] exportAttendances(List<DailyAttendanceResponse> attendances) throws IOException {
        org.springframework.core.io.ClassPathResource resource = new org.springframework.core.io.ClassPathResource("templates/export/attendance_list.xlsx");
        try (InputStream is = resource.getInputStream()) {
            java.util.Map<String, Object> singleValues = new java.util.HashMap<>();
            java.util.List<java.util.Map<String, Object>> items = new java.util.ArrayList<>();
            
            System.out.println("Exporting " + attendances.size() + " attendances");
            for (DailyAttendanceResponse att : attendances) {
                java.util.Map<String, Object> item = new java.util.HashMap<>();
                if (att.getEmployee() != null) {
                    item.put("empCode", att.getEmployee().getCode());
                    item.put("fullName", att.getEmployee().getFullName());
                } else {
                    item.put("empCode", "");
                    item.put("fullName", "");
                }
                item.put("date", att.getAttendanceDate() != null ? att.getAttendanceDate().toString() : "");
                item.put("origTeam", att.getOriginalTeam() != null ? att.getOriginalTeam().getName() : "");
                item.put("actTeam", att.getActualTeam() != null ? att.getActualTeam().getName() : "");
                item.put("symbol", att.getAttendanceDefinition() != null ? att.getAttendanceDefinition().getCode() : "");
                items.add(item);
            }
            
            java.util.Map<String, List<java.util.Map<String, Object>>> listData = new java.util.HashMap<>();
            listData.put("item", items);
            return excelTemplateFiller.fillTemplate(is, singleValues, listData);
        }
    }

    public byte[] exportAttendanceMatrix(List<DailyAttendanceResponse> attendances, List<LocalDate> dates) throws IOException {
        org.springframework.core.io.ClassPathResource resource = new org.springframework.core.io.ClassPathResource("templates/export/attendance_matrix.xlsx");
        try (InputStream is = resource.getInputStream()) {
            java.util.Map<String, Object> singleValues = new java.util.HashMap<>();
            if (!dates.isEmpty()) {
                singleValues.put("month", dates.get(0).getMonthValue());
                singleValues.put("year", dates.get(0).getYear());
            }

            java.util.Map<Long, List<DailyAttendanceResponse>> employeeMap = attendances.stream()
                .collect(java.util.stream.Collectors.groupingBy(a -> a.getEmployee().getId()));
            
            List<com.plywood.payroll.modules.employee.dto.response.EmployeeResponse> employees = attendances.stream()
                .map(DailyAttendanceResponse::getEmployee)
                .distinct()
                .sorted(java.util.Comparator.comparing(e -> e.getTeam() != null ? e.getTeam().getName() : ""))
                .collect(java.util.stream.Collectors.toList());

            java.util.List<java.util.Map<String, Object>> items = new java.util.ArrayList<>();
            for (com.plywood.payroll.modules.employee.dto.response.EmployeeResponse emp : employees) {
                java.util.Map<String, Object> item = new java.util.HashMap<>();
                item.put("empCode", emp.getCode());
                item.put("fullName", emp.getFullName());
                item.put("team", emp.getTeam() != null ? emp.getTeam().getName() : "");
                
                List<DailyAttendanceResponse> empAtts = employeeMap.getOrDefault(emp.getId(), new ArrayList<>());
                int totalNc = 0;
                for (int i = 0; i < dates.size(); i++) {
                    LocalDate date = dates.get(i);
                    DailyAttendanceResponse att = empAtts.stream()
                        .filter(a -> a.getAttendanceDate().equals(date))
                        .findFirst().orElse(null);
                    
                    String symbol = "";
                    if (att != null && att.getAttendanceDefinition() != null) {
                        symbol = att.getAttendanceDefinition().getCode();
                        totalNc++; 
                    }
                    item.put("d" + (i + 1), symbol); 
                }
                item.put("total", totalNc);
                items.add(item);
            }
            java.util.Map<String, List<java.util.Map<String, Object>>> listData = new java.util.HashMap<>();
            listData.put("item", items);
            
            return excelTemplateFiller.fillTemplate(is, singleValues, listData);
        }
    }


    public byte[] getImportTemplate() throws IOException {
        try {
            return getTemplateBytes(ATTENDANCE_IMPORT_TEMPLATE);
        } catch (IOException e) {
            return generateDynamicAttendanceTemplate();
        }
    }

    public byte[] getProductImportTemplate() throws IOException {
        return getTemplateBytes(PRODUCT_IMPORT_TEMPLATE);
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
            String[] headers = {"Mã NV", "Họ Tên", "Số điện thoại", "Số CCCD", "Tên đăng nhập", "Mật khẩu", "Phòng ban", "Tổ đội", "Chức vụ", "Quyền Đăng nhập (Y/N)"};

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
            ExcelHelper.addDataValidation(sheet, depts, 1, 1000, 6, 6);

            String[] teams = teamRepository.findAll().stream()
                    .map(com.plywood.payroll.modules.organization.entity.Team::getName)
                    .toArray(String[]::new);
            ExcelHelper.addDataValidation(sheet, teams, 1, 1000, 7, 7);

            String[] roles = roleRepository.findAll().stream()
                    .map(com.plywood.payroll.modules.organization.entity.Role::getName)
                    .toArray(String[]::new);
            ExcelHelper.addDataValidation(sheet, roles, 1, 1000, 8, 8);

            ExcelHelper.addDataValidation(sheet, new String[]{"Y", "N", "Có", "Không"}, 1, 1000, 9, 9);

            return ExcelHelper.createWorkbookBytes(workbook);
        }
    }

    public ImportResult<DailyAttendanceRequest> importAttendances(MultipartFile file) throws IOException {
        List<DailyAttendanceRequest> requests = new ArrayList<>();
        List<ImportError> errors = new ArrayList<>();
        
        try (InputStream is = file.getInputStream(); Workbook workbook = ExcelHelper.createWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            if (!rows.hasNext()) {
                errors.add(ImportError.builder().errorMessage("File trống hoặc sai định dạng").build());
                return ImportResult.<DailyAttendanceRequest>builder().errors(errors).errorCount(1).build();
            }

            Row headerRow = rows.next();
            // Validate header
            String[] expectedHeaders = {"Mã NV", "Họ Tên", "Ngày", "Tổ biên chế", "Tổ thực tế", "Mã loại công"};
            for (int i = 0; i < expectedHeaders.length; i++) {
                String actual = ExcelHelper.getCellValueAsString(headerRow.getCell(i));
                if (actual == null || !actual.toLowerCase().contains(expectedHeaders[i].toLowerCase())) {
                    errors.add(ImportError.builder()
                            .rowNumber(1)
                            .columnName(expectedHeaders[i])
                            .errorMessage("Tiêu đề cột không khớp. Mong đợi: " + expectedHeaders[i])
                            .build());
                    return ImportResult.<DailyAttendanceRequest>builder().errors(errors).errorCount(1).build();
                }
            }

            int rowIdx = 1;
            while (rows.hasNext()) {
                rowIdx++;
                Row currentRow = rows.next();
                if (isRowEmpty(currentRow)) continue;

                String empCode = ExcelHelper.getCellValueAsString(currentRow.getCell(0));
                if (empCode == null || empCode.isEmpty()) {
                    errors.add(ImportError.builder()
                            .rowNumber(rowIdx)
                            .columnName("Mã NV")
                            .errorMessage("Mã nhân viên không được để trống")
                            .build());
                    continue;
                }

                var empOpt = employeeRepository.findByCode(empCode);
                if (empOpt.isEmpty()) {
                    errors.add(ImportError.builder()
                            .rowNumber(rowIdx)
                            .columnName("Mã NV")
                            .cellValue(empCode)
                            .errorMessage("Không tìm thấy nhân viên với mã này")
                            .build());
                    continue;
                }

                var emp = empOpt.get();
                DailyAttendanceRequest req = new DailyAttendanceRequest();
                req.setEmployeeId(emp.getId());

                try {
                    String dateStr = ExcelHelper.getCellValueAsString(currentRow.getCell(2));
                    req.setAttendanceDate(dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now());
                } catch (Exception e) {
                    errors.add(ImportError.builder()
                            .rowNumber(rowIdx)
                            .columnName("Ngày")
                            .errorMessage("Ngày không đúng định dạng YYYY-MM-DD")
                            .build());
                    continue;
                }

                String origTeamName = ExcelHelper.getCellValueAsString(currentRow.getCell(3));
                if (origTeamName != null && !origTeamName.isEmpty()) {
                    var teamOpt = teamRepository.findByName(origTeamName);
                    if (teamOpt.isPresent()) req.setOriginalTeamId(teamOpt.get().getId());
                    else {
                         errors.add(ImportError.builder().rowNumber(rowIdx).columnName("Tổ biên chế").cellValue(origTeamName).errorMessage("Không tìm thấy tổ này").build());
                         continue;
                    }
                }

                String actualTeamName = ExcelHelper.getCellValueAsString(currentRow.getCell(4));
                if (actualTeamName != null && !actualTeamName.isEmpty()) {
                    var teamOpt = teamRepository.findByName(actualTeamName);
                    if (teamOpt.isPresent()) req.setActualTeamId(teamOpt.get().getId());
                    else {
                        errors.add(ImportError.builder().rowNumber(rowIdx).columnName("Tổ thực tế").cellValue(actualTeamName).errorMessage("Không tìm thấy tổ này").build());
                        continue;
                    }
                } else {
                    req.setActualTeamId(req.getOriginalTeamId());
                }

                String defCode = ExcelHelper.getCellValueAsString(currentRow.getCell(5));
                if (defCode != null && !defCode.isEmpty()) {
                    var defOpt = attendanceDefinitionRepository.findByCode(defCode);
                    if (defOpt.isPresent()) req.setAttendanceDefinitionId(defOpt.get().getId());
                    else {
                        errors.add(ImportError.builder().rowNumber(rowIdx).columnName("Mã loại công").cellValue(defCode).errorMessage("Mã loại công không tồn tại").build());
                        continue;
                    }
                } else {
                     errors.add(ImportError.builder().rowNumber(rowIdx).columnName("Mã loại công").errorMessage("Thiếu mã loại công").build());
                     continue;
                }

                requests.add(req);
            }
        }
        return ImportResult.<DailyAttendanceRequest>builder()
                .data(requests)
                .errors(errors)
                .successCount(requests.size())
                .errorCount(errors.size())
                .build();
    }

    public ImportResult<EmployeeRequest> importEmployeesPreview(MultipartFile file) throws IOException {
        List<EmployeeRequest> data = new ArrayList<>();
        List<ImportError> errors = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = ExcelHelper.createWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            // Validate headers
            String[] expectedHeaders = {"Mã NV", "Họ Tên", "Số điện thoại", "Số CCCD", "Tên đăng nhập", "Mật khẩu", "Phòng ban", "Tổ đội", "Chức vụ", "Quyền Đăng nhập"};
            if (headerRow == null || !validateHeaders(headerRow, expectedHeaders)) {
                throw new IOException("Tiêu đề cột không khớp. Vui lòng sử dụng file mẫu đúng định dạng.");
            }

            Iterator<Row> rows = sheet.iterator();
            if (rows.hasNext()) rows.next(); // Skip header

            int rowIdx = 1;
            while (rows.hasNext()) {
                rowIdx++;
                Row row = rows.next();
                if (isRowEmpty(row)) continue;

                String code = ExcelHelper.getCellValueAsString(row.getCell(0));
                String name = ExcelHelper.getCellValueAsString(row.getCell(1));
                String phone = ExcelHelper.getCellValueAsString(row.getCell(2));
                String citizenId = ExcelHelper.getCellValueAsString(row.getCell(3));
                String username = ExcelHelper.getCellValueAsString(row.getCell(4));
                String password = ExcelHelper.getCellValueAsString(row.getCell(5));
                String deptName = ExcelHelper.getCellValueAsString(row.getCell(6));
                String teamName = ExcelHelper.getCellValueAsString(row.getCell(7));
                String roleName = ExcelHelper.getCellValueAsString(row.getCell(8));
                String canLoginStr = ExcelHelper.getCellValueAsString(row.getCell(9));

                if (name == null || name.trim().isEmpty()) {
                    errors.add(new ImportError(rowIdx, "Họ Tên", "", "Họ tên không được để trống"));
                    continue;
                }

                EmployeeRequest req = new EmployeeRequest();
                req.setCode(code);
                req.setFullName(name);
                req.setPhone(phone);
                req.setCitizenId(citizenId);
                req.setUsername(username);
                req.setPassword(password);
                req.setCanLogin("Y".equalsIgnoreCase(canLoginStr) || "Có".equalsIgnoreCase(canLoginStr));

                // Department
                if (deptName != null && !deptName.trim().isEmpty()) {
                    var dept = departmentRepository.findByName(deptName);
                    if (dept.isPresent()) req.setDepartmentId(dept.get().getId());
                    else errors.add(new ImportError(rowIdx, "Phòng ban", deptName, "Phòng ban không tồn tại"));
                }

                // Team
                if (teamName != null && !teamName.trim().isEmpty()) {
                    var team = teamRepository.findByName(teamName);
                    if (team.isPresent()) req.setTeamId(team.get().getId());
                    else errors.add(new ImportError(rowIdx, "Tổ đội", teamName, "Tổ đội không tồn tại"));
                }

                // Role
                if (roleName != null && !roleName.trim().isEmpty()) {
                    var role = roleRepository.findByName(roleName);
                    if (role.isPresent()) req.setRoleId(role.get().getId());
                    else errors.add(new ImportError(rowIdx, "Chức vụ", roleName, "Chức vụ không tồn tại"));
                }

                // Check duplicate code if provided
                final int currentRowIdx = rowIdx;
                if (code != null && !code.trim().isEmpty() && employeeRepository.findByCode(code).isPresent()) {
                    errors.add(new ImportError(currentRowIdx, "Mã NV", code, "Mã nhân viên đã tồn tại"));
                }

                if (errors.stream().noneMatch(e -> e.getRowNumber() == currentRowIdx)) {
                    data.add(req);
                }
            }
        }

        return ImportResult.<EmployeeRequest>builder()
                .data(data)
                .errors(errors)
                .successCount(data.size())
                .errorCount(errors.size())
                .build();
    }

    public List<EmployeeRequest> importEmployees(MultipartFile file) throws IOException {
        return importEmployeesPreview(file).getData();
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

    public ImportResult<DepartmentRequest> importDepartments(MultipartFile file) throws IOException {
        List<DepartmentRequest> data = new ArrayList<>();
        List<ImportError> errors = new ArrayList<>();
        
        try (InputStream is = file.getInputStream(); Workbook workbook = ExcelHelper.createWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            
            // Validate headers
            String[] expectedHeaders = {"Tên phòng ban"};
            if (headerRow == null || !validateHeaders(headerRow, expectedHeaders)) {
                throw new IOException("Tiêu đề cột không khớp. Yêu cầu: Tên phòng ban");
            }

            Iterator<Row> rows = sheet.iterator();
            if (rows.hasNext()) rows.next(); // Skip header
            
            int rowIdx = 1;
            while (rows.hasNext()) {
                rowIdx++;
                Row row = rows.next();
                if (isRowEmpty(row)) continue;
                
                String name = ExcelHelper.getCellValueAsString(row.getCell(0));
                
                if (name == null || name.trim().isEmpty()) {
                    errors.add(new ImportError(rowIdx, "Tên phòng ban", "", "Tên phòng ban không được để trống"));
                    continue;
                }
                
                if (departmentRepository.findByName(name).isPresent()) {
                    errors.add(new ImportError(rowIdx, "Tên phòng ban", name, "Phòng ban này đã tồn tại trong hệ thống"));
                    continue;
                }

                DepartmentRequest req = new DepartmentRequest();
                req.setName(name);
                data.add(req);
            }
        }
        
        return ImportResult.<DepartmentRequest>builder()
                .data(data)
                .errors(errors)
                .successCount(data.size())
                .errorCount(errors.size())
                .build();
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
                String name = ExcelHelper.getCellValueAsString(row.getCell(0));
                String code = ExcelHelper.getCellValueAsString(row.getCell(1));
                String thickness = ExcelHelper.getCellValueAsString(row.getCell(2));
                String length = ExcelHelper.getCellValueAsString(row.getCell(3));
                String width = ExcelHelper.getCellValueAsString(row.getCell(4));
                String coatingStr = ExcelHelper.getCellValueAsString(row.getCell(5));
                String unitName = ExcelHelper.getCellValueAsString(row.getCell(6));

                if (code != null && !code.isEmpty()) {
                    ProductRequest req = new ProductRequest();
                    req.setName(name != null ? name : code);
                    req.setCode(code);
                    req.setThickness(thickness != null ? new java.math.BigDecimal(thickness) : java.math.BigDecimal.ZERO);
                    req.setLength(length != null ? new java.math.BigDecimal(length) : java.math.BigDecimal.ZERO);
                    req.setWidth(width != null ? new java.math.BigDecimal(width) : java.math.BigDecimal.ZERO);
                    
                    // Map FilmCoatingType
                    FilmCoatingType coating = FilmCoatingType.NONE;
                    if (coatingStr != null) {
                        if (coatingStr.contains("1 mặt")) coating = FilmCoatingType.SIDE_1;
                        else if (coatingStr.contains("2 mặt")) coating = FilmCoatingType.SIDE_2;
                    }
                    req.setFilmCoatingType(coating);
                    
                    // Map unit by name
                    if (unitName != null && !unitName.isEmpty()) {
                        productUnitRepository.findByName(unitName).ifPresent(u -> req.setUnitId(u.getId()));
                    }
                    
                    // Default to 'Tấm' if unit is missing and exists in DB
                    if (req.getUnitId() == null) {
                        productUnitRepository.findByName("Tấm").ifPresent(u -> req.setUnitId(u.getId()));
                    }

                    if (req.getUnitId() != null) {
                        requests.add(req);
                    }
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
        org.springframework.core.io.ClassPathResource resource = new org.springframework.core.io.ClassPathResource("templates/export/production_list.xlsx");
        try (InputStream is = resource.getInputStream()) {
            java.util.Map<String, Object> singleValues = new java.util.HashMap<>();
            java.util.List<java.util.Map<String, Object>> items = new java.util.ArrayList<>();
            
            for (ProductionRecordResponse r : records) {
                java.util.Map<String, Object> item = new java.util.HashMap<>();
                item.put("date", r.getProductionDate().toString());
                item.put("product", r.getProduct() != null ? r.getProduct().getCode() : "");
                item.put("quality", r.getQuality() != null ? r.getQuality().getCode() : "");
                item.put("size", r.getProduct() != null ? r.getProduct().getThickness() + "x" + r.getProduct().getLength() + "x" + r.getProduct().getWidth() : "");
                item.put("layer", ""); 
                item.put("team", r.getTeam() != null ? r.getTeam().getName() : "");
                item.put("qty", r.getQuantity() != null ? r.getQuantity() : 0);
                
                // Fetch price for list (optional, but keep it simple for now)
                item.put("price", 0);
                item.put("total", 0);
                item.put("note", "");
                items.add(item);
            }
            
            java.util.Map<String, List<java.util.Map<String, Object>>> listData = new java.util.HashMap<>();
            listData.put("item", items);
            return excelTemplateFiller.fillTemplate(is, singleValues, listData);
        }
    }

    public byte[] exportProductionRecordMatrix(List<ProductionRecordResponse> records, List<LocalDate> dates) throws IOException {
        org.springframework.core.io.ClassPathResource resource = new org.springframework.core.io.ClassPathResource("templates/export/production_matrix.xlsx");
        
        // Nhóm dữ liệu theo tổ
        java.util.Map<Long, List<ProductionRecordResponse>> teamMap = records.stream()
            .filter(r -> r.getTeam() != null)
            .collect(java.util.stream.Collectors.groupingBy(r -> r.getTeam().getId()));

        java.util.List<java.util.Map<String, Object>> sheetsData = new java.util.ArrayList<>();
        
        for (java.util.Map.Entry<Long, List<ProductionRecordResponse>> entry : teamMap.entrySet()) {
            List<ProductionRecordResponse> teamRecords = entry.getValue();
            com.plywood.payroll.modules.organization.dto.response.TeamResponse team = teamRecords.get(0).getTeam();
            
            java.util.Map<String, Object> sheetData = new java.util.HashMap<>();
            sheetData.put("sheetName", team.getName());
            sheetData.put("team", team.getName());
            sheetData.put("month", dates.isEmpty() ? "" : dates.get(0).getMonthValue());
            sheetData.put("year", dates.isEmpty() ? "" : dates.get(0).getYear());

            // Phân tích các sản phẩm duy nhất của tổ này trong tháng
            List<ColumnMeta> distinctMetas = teamRecords.stream()
                .map(r -> new ColumnMeta(r.getProduct(), r.getQuality()))
                .distinct()
                .collect(java.util.stream.Collectors.toList());
            
            // Điền tên sản phẩm vào header (prod_1_name...)
            for (int i = 0; i < 20; i++) {
                if (i < distinctMetas.size()) {
                    ColumnMeta meta = distinctMetas.get(i);
                    String name = meta.getProduct().getCode() + " (" + meta.getQuality().getCode() + ")";
                    sheetData.put("prod_" + (i + 1) + "_name", name);
                } else {
                    sheetData.put("prod_" + (i + 1) + "_name", "");
                }
            }

            java.util.List<java.util.Map<String, Object>> items = new java.util.ArrayList<>();
            for (LocalDate date : dates) {
                java.util.Map<String, Object> item = new java.util.HashMap<>();
                item.put("date", date.getDayOfMonth());
                
                for (int i = 0; i < 20; i++) {
                    if (i < distinctMetas.size()) {
                        ColumnMeta meta = distinctMetas.get(i);
                        int qty = teamRecords.stream()
                            .filter(r -> r.getProductionDate().equals(date) 
                                     && r.getProduct().getId().equals(meta.getProduct().getId())
                                     && r.getQuality().getId().equals(meta.getQuality().getId()))
                            .mapToInt(r -> r.getQuantity() != null ? r.getQuantity() : 0)
                            .sum();
                        item.put("p" + (i + 1), qty > 0 ? qty : "");
                    } else {
                        item.put("p" + (i + 1), "");
                    }
                }
                item.put("penalty", 0);
                item.put("total", 0);
                items.add(item);
            }
            
            java.util.Map<String, List<java.util.Map<String, Object>>> listMap = new java.util.HashMap<>();
            listMap.put("item", items);
            sheetData.put("_lists", listMap);
            sheetsData.add(sheetData);
        }

        try (InputStream is = resource.getInputStream()) {
            return excelTemplateFiller.fillMultiSheetTemplate(is, sheetsData, "sheetName");
        }
    }

    public byte[] exportPayslips(int month, int year, List<PayrollItemResponse> items, java.util.Map<Long, List<PayrollDailyDetailResponse>> detailsMap) throws IOException {
        org.springframework.core.io.ClassPathResource resource = new org.springframework.core.io.ClassPathResource("templates/export/payslip.xlsx");
        
        java.util.List<java.util.Map<String, Object>> sheetsData = new java.util.ArrayList<>();
        
        for (PayrollItemResponse item : items) {
            java.util.Map<String, Object> sheetData = new java.util.HashMap<>();
            sheetData.put("sheetName", item.getEmployeeName());
            sheetData.put("month", month);
            sheetData.put("year", year);
            sheetData.put("fullName", item.getEmployeeName());
            sheetData.put("empCode", item.getEmployeeCode());
            sheetData.put("department", item.getDepartmentName() != null ? item.getDepartmentName() : "");
            sheetData.put("team", item.getTeamName() != null ? item.getTeamName() : "");
            
            sheetData.put("prodSalary", item.getProductSalary());
            sheetData.put("benefit", item.getBenefitSalary());
            sheetData.put("bonusPenalty", item.getTotalPenaltyBonus());
            sheetData.put("netPay", item.getTotalSalary());

            java.util.List<java.util.Map<String, Object>> itemDetails = new java.util.ArrayList<>();
            List<PayrollDailyDetailResponse> details = detailsMap.get(item.getId());
            if (details != null) {
                for (PayrollDailyDetailResponse d : details) {
                    java.util.Map<String, Object> detail = new java.util.HashMap<>();
                    detail.put("date", d.getDate() != null ? d.getDate().toString() : "");
                    detail.put("symbol", d.getAttendanceSymbol() != null ? d.getAttendanceSymbol() : "");
                    detail.put("team", d.getTeamName() != null ? d.getTeamName() : "");
                    detail.put("prod", d.getProductSalary());
                    detail.put("ben", d.getBenefitSalary());
                    
                    double bonus = d.getBonus() != null ? d.getBonus().doubleValue() : 0;
                    double penalty = d.getPenalty() != null ? d.getPenalty().doubleValue() : 0;
                    detail.put("netpen", bonus - penalty);
                    itemDetails.add(detail);
                }
            }
            
            java.util.Map<String, List<java.util.Map<String, Object>>> listMap = new java.util.HashMap<>();
            listMap.put("item", itemDetails);
            sheetData.put("_lists", listMap);
            sheetsData.add(sheetData);
        }

        try (InputStream is = resource.getInputStream()) {
            return excelTemplateFiller.fillMultiSheetTemplate(is, sheetsData, "sheetName");
        }
    }

    private boolean validateHeaders(Row headerRow, String[] expectedHeaders) {
        for (int i = 0; i < expectedHeaders.length; i++) {
            Cell cell = headerRow.getCell(i);
            String actual = ExcelHelper.getCellValueAsString(cell);
            if (actual == null || !actual.trim().equalsIgnoreCase(expectedHeaders[i].trim())) {
                return false;
            }
        }
        return true;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        // Check if all cells in the row are blank or empty
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK &&
                (cell.getCellType() != CellType.STRING || !cell.getStringCellValue().trim().isEmpty())) {
                return false;
            }
        }
        return true;
    }

    @lombok.Data
    private static class ColumnMeta {
        private final com.plywood.payroll.modules.product.dto.response.ProductResponse product;
        private final com.plywood.payroll.modules.quality.dto.response.ProductQualityResponse quality;

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
}
