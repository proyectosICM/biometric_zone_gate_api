package com.icm.biometric_zone_gate_api.services;

import com.icm.biometric_zone_gate_api.models.CompanyModel;
import com.icm.biometric_zone_gate_api.repositories.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    public List<CompanyModel> getAllCompanies() {
        return companyRepository.findAll();
    }

    public Page<CompanyModel> getAllCompanies(Pageable pageable) {
        return companyRepository.findAll(pageable);
    }

    public Optional<CompanyModel> getCompanyById(Long id) {
        return companyRepository.findById(id);
    }

    public CompanyModel createCompany(CompanyModel company) {
        return companyRepository.save(company);
    }

    public Optional<CompanyModel> updateCompany(Long id, CompanyModel updatedCompany) {
        return companyRepository.findById(id).map(company -> {
            company.setName(updatedCompany.getName());
            return companyRepository.save(company);
        });
    }

    public boolean deleteCompany(Long id) {
        if (companyRepository.existsById(id)) {
            companyRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
