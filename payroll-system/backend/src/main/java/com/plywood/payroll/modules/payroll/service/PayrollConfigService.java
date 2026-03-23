package com.plywood.payroll.modules.payroll.service;

import com.plywood.payroll.modules.payroll.entity.PayrollConfig;
import com.plywood.payroll.modules.payroll.repository.PayrollConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PayrollConfigService {

    private final PayrollConfigRepository configRepository;

    public List<PayrollConfig> getAll(Integer month, Integer year) {
        if (month != null && year != null) {
            List<PayrollConfig> configs = new java.util.ArrayList<>();
            
            // Lấy các key cấu hình phổ biến
            String[] keys = {"MIN_ATTENDANCE_DAYS", "FILM_SURCHARGE_1_SIDE", "FILM_SURCHARGE_2_SIDE"};
            for (String key : keys) {
                configs.add(configRepository.findEffectiveConfig(key, month, year)
                        .orElseGet(() -> {
                            PayrollConfig c = new PayrollConfig();
                            c.setConfigKey(key);
                            c.setConfigValue("0");
                            c.setMonth(month);
                            c.setYear(year);
                            return c;
                        }));
            }
            return configs;
        }
        return configRepository.findAll();
    }

    public String getEffectiveValue(String key, Integer month, Integer year) {
        return configRepository.findEffectiveConfig(key, month, year)
                .map(PayrollConfig::getConfigValue)
                .orElse("0");
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
