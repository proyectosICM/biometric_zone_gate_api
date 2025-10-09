package com.icm.biometric_zone_gate_api.repositories;

import com.icm.biometric_zone_gate_api.enums.AccessType;
import com.icm.biometric_zone_gate_api.models.AccessLogsModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccessLogsRepository extends JpaRepository<AccessLogsModel, Long> {

    List<AccessLogsModel> findByUserId(Long userId);

    Page<AccessLogsModel> findByUserId(Long userId, Pageable pageable);

    List<AccessLogsModel> findByDeviceId(Long deviceId);

    Page<AccessLogsModel> findByDeviceId(Long deviceId, Pageable pageable);

    List<AccessLogsModel> findByCompanyId(Long companyId);

    Page<AccessLogsModel> findByCompanyId(Long companyId, Pageable pageable);

    List<AccessLogsModel> findByAction(AccessType action);

    Page<AccessLogsModel> findByAction(AccessType action, Pageable pageable);
}
