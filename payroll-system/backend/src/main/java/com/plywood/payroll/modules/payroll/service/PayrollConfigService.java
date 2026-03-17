package com.plywood.payroll.modules.payroll.service;

import com.plywood.payroll.modules.payroll.entity.PayrollConfig;
import com.plywood.payroll.shared.exception.ResourceNotFoundException;
import com.plywood.payroll.modules.payroll.repository.PayrollConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PayrollConfigService {

    private final PayrollConfigRepository configRepository;

    public List<PayrollConfig> getAll() {
        return configRepository.findAll();
    }

    public String getEffectiveValue(String key, Integer month, Integer year) {
        return configRepository.findEffectiveConfig(key, month, year)
                .map(PayrollConfig::getConfigValue)
                .orElseThrow(() -> new ResourceNotFoundException("Cấu hình " + key, "tháng " + month + "/" + year));
    }

    @Transactional
    public PayrollConfig update(String key, String value, Integer month, Integer year) {
        PayrollConfig config = configRepository.findByConfigKeyAndMonthAndYear(key, month, year)
                .orElseGet(() -> {
                    PayrollConfig newConfig = new PayrollConfig();
                    newConfig.setConfigKey(key);
                    newConfig.setMonth(month);
                    newConfig.setYear(year);
                    return newConfig;
                });
        config.setConfigValue(value);
        return configRepository.save(config);
    }
}
