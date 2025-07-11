package com.icm.biometric_zone_gate_api.repositories;

import com.icm.biometric_zone_gate_api.models.DeviceModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceRepository extends JpaRepository<DeviceModel, Long> {
}
