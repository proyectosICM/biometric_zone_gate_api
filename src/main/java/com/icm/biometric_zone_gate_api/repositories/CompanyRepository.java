package com.icm.biometric_zone_gate_api.repositories;

import com.icm.biometric_zone_gate_api.models.CompanyModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<CompanyModel, Long> {
}
