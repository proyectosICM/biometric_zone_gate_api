package com.icm.biometric_zone_gate_api.repositories;

import com.icm.biometric_zone_gate_api.enums.AccessType;
import com.icm.biometric_zone_gate_api.models.AccessLogsModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccessLogsRepository extends JpaRepository<AccessLogsModel, Long> {

    List<AccessLogsModel> findByUserId(Long userId);

    List<AccessLogsModel> findByDeviceId(Long deviceId);

    List<AccessLogsModel> findByCompanyId(Long companyId);

    List<AccessLogsModel> findByAction(AccessType action);
}
