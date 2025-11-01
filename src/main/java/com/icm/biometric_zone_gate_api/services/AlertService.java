package com.icm.biometric_zone_gate_api.services;

import com.icm.biometric_zone_gate_api.models.AlertModel;
import com.icm.biometric_zone_gate_api.models.CompanyModel;
import com.icm.biometric_zone_gate_api.repositories.AlertRepository;
import com.icm.biometric_zone_gate_api.repositories.CompanyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final CompanyRepository companyRepository;

    public AlertModel getById(Long id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Alert not found: " + id));
    }

    public List<AlertModel> findAll() {
        return alertRepository.findAll();
    }

    public Page<AlertModel> findAll(Pageable pageable) {
        return alertRepository.findAll(pageable);
    }

    public List<AlertModel> findByCompany(Long companyId) {
        return alertRepository.findByCompanyId(companyId);
    }

    public Page<AlertModel> findByCompany(Long companyId, Pageable pageable) {
        return alertRepository.findByCompanyId(companyId, pageable);
    }

    @Transactional
    public AlertModel save(AlertModel alert) {
        if (alert.getCompany() == null || alert.getCompany().getId() == null) {
            throw new IllegalArgumentException("Alert.company.id es requerido");
        }

        CompanyModel company = companyRepository.findById(alert.getCompany().getId())
                .orElseThrow(() -> new NotFoundException("Company not found: " + alert.getCompany().getId()));
        alert.setCompany(company);
        return alertRepository.save(alert);
    }

    @Transactional
    public AlertModel update(Long id, AlertModel alertModel) {
        AlertModel existing = alertRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Alert not found: " + id));

        if (alertModel.getTitle() != null) {
            existing.setTitle(alertModel.getTitle());
        }
        if (alertModel.getDescription() != null) {
            existing.setDescription(alertModel.getDescription());
        }
        return alertRepository.save(existing);
    }

    @Transactional
    public void deleteById(Long id) {
        if (!alertRepository.existsById(id)) {
            throw new NotFoundException("Alert not found: " + id);
        }
        alertRepository.deleteById(id);
    }


    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String msg) { super(msg); }
    }
}
