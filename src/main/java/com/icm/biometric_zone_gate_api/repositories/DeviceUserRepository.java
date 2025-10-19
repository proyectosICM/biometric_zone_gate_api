package com.icm.biometric_zone_gate_api.repositories;

import com.icm.biometric_zone_gate_api.models.DeviceUserModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceUserRepository extends JpaRepository<DeviceUserModel, Long> {
    //List<DeviceUserModel> findByUserId(Long userId);
    //List<DeviceUserModel> findByDeviceId(Long deviceId);
    Optional<DeviceUserModel> findByDeviceIdAndEnrollId(Long deviceId, Integer enrollId);
}
