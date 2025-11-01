package com.icm.biometric_zone_gate_api.repositories;

import com.icm.biometric_zone_gate_api.models.AlertModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<AlertModel, Long> {
    List<AlertModel> findByCompanyId(Long id);

    Page<AlertModel> findByCompanyId(Long id, Pageable pageable);
}
