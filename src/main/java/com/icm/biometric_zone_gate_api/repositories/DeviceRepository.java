package com.icm.biometric_zone_gate_api.repositories;

import com.icm.biometric_zone_gate_api.models.DeviceModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<DeviceModel, Long> {
    DeviceModel findByName(String name);
    Optional<DeviceModel> findBySn(String sn);

    List<DeviceModel> findByCompanyId(Long companyId);

    Page<DeviceModel> findByCompanyId(Long companyId, Pageable pageable);
}
