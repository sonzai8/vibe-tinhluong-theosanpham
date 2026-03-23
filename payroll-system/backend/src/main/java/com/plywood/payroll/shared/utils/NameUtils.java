package com.plywood.payroll.shared.utils;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class NameUtils {
    /**
     * Tạo prefix cho mã nhân viên từ họ tên.
     * Ví dụ: Phan Văn Sơn -> SONPV
     * Quy tắc: Phần TÊN (cuối cùng) viết hoa + Chữ cái đầu của HỌ và ĐỆM viết hoa.
     */
    public static String generateCodePrefix(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "EMP";
        
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 0) return "EMP";

        StringBuilder prefix = new StringBuilder();
        
        // Lấy phần Tên (phần cuối cùng)
        String lastName = normalize(parts[parts.length - 1]);
        prefix.append(lastName.toUpperCase());

        // Lấy chữ cái đầu của các phần còn lại (Họ và Tên đệm)
        for (int i = 0; i < parts.length - 1; i++) {
            String part = normalize(parts[i]);
            if (!part.isEmpty()) {
                prefix.append(Character.toUpperCase(part.charAt(0)));
            }
        }

        return prefix.toString();
    }

    /**
     * Loại bỏ dấu tiếng Việt và chuẩn hóa chuỗi.
     */
    public static String normalize(String str) {
        if (str == null) return "";
        String temp = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replaceAll("[^a-zA-Z0-9]", "");
    }
}
