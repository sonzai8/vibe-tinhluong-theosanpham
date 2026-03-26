package com.plywood.payroll.modules.system.service;

import com.plywood.payroll.modules.system.entity.SystemLog;
import com.plywood.payroll.modules.system.repository.SystemLogRepository;
import com.plywood.payroll.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemLogService {
    
    private final SystemLogRepository systemLogRepository;

    public void log(String type, String message, String detail) {
        SystemLog log = new SystemLog();
        log.setType(type);
        log.setMessage(message);
        log.setDetail(detail);
        systemLogRepository.save(log);
    }

    public Page<SystemLog> getAll(Pageable pageable) {
        return systemLogRepository.findAll(pageable);
    }

    @Transactional
    public SystemLog resolve(Long id, String note) {
        SystemLog log = systemLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("System Log", id));
        log.setResolved(true);
        log.setResolutionNote(note);
        return systemLogRepository.save(log);
    }
}
