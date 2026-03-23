package com.plywood.payroll.modules.penalty.service;

import com.plywood.payroll.modules.penalty.entity.PenaltyBonusType;
import com.plywood.payroll.modules.penalty.repository.PenaltyBonusTypeRepository;
import com.plywood.payroll.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PenaltyBonusTypeService {

    private final PenaltyBonusTypeRepository repository;

    public List<PenaltyBonusType> getAll() {
        return repository.findAll();
    }

    public PenaltyBonusType getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loại thưởng/phạt", id));
    }

    @Transactional
    public PenaltyBonusType create(PenaltyBonusType type) {
        return repository.save(type);
    }

    @Transactional
    public PenaltyBonusType update(Long id, PenaltyBonusType typeDetails) {
        PenaltyBonusType type = getById(id);
        type.setName(typeDetails.getName());
        type.setDefaultAmount(typeDetails.getDefaultAmount());
        type.setDescription(typeDetails.getDescription());
        return repository.save(type);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Loại thưởng/phạt", id);
        }
        repository.deleteById(id);
    }
}
