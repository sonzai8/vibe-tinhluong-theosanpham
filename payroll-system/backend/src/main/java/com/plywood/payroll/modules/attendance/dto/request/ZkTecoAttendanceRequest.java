package com.plywood.payroll.modules.attendance.dto.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ZkTecoAttendanceRequest {
    private String zkDeviceId; // Corresponds to UserID on the machine
    private LocalDateTime checkTime;
    private String verifyMode; // optional
    private String doorId; // optional
}
