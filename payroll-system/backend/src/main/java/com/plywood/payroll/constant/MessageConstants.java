package com.plywood.payroll.constant;

public final class MessageConstants {
    
    private MessageConstants() {
        // Prevent instantiation
    }

    public static final String SUCCESS_GET_LIST = "Lấy danh sách thành công";
    public static final String SUCCESS_GET_DETAIL = "Lấy thông tin thành công";
    public static final String SUCCESS_CREATE = "Tạo mới thành công";
    public static final String SUCCESS_UPDATE = "Cập nhật thành công";
    public static final String SUCCESS_DELETE = "Xóa thành công";
    public static final String SUCCESS_LOGIN = "Đăng nhập thành công";
    public static final String SUCCESS_REGISTER = "Đăng ký thành công";

    public static final String SUCCESS_CONFIRM_PAYROLL = "Đã chốt bảng lương";
    public static final String SUCCESS_CALCULATE_PAYROLL = "Tính lương thành công";
    
    public static final String ERR_RESOURCE_NOT_FOUND = "Không tìm thấy %s với ID: %d";
    public static final String ERR_USERNAME_EXISTED = "Tên đăng nhập đã tồn tại";
    public static final String ERR_EMPLOYEE_CODE_EXISTED = "Mã nhân viên đã tồn tại";

}
