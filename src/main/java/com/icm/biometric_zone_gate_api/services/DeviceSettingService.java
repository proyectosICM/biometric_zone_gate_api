package com.icm.biometric_zone_gate_api.services;

import com.icm.biometric_zone_gate_api.dto.DeviceSettingDTO;
import com.icm.biometric_zone_gate_api.models.DeviceSettingModel;

import java.util.Optional;

public interface DeviceSettingService {
    Optional<DeviceSettingDTO> findById(Long id);
    DeviceSettingDTO save(DeviceSettingDTO dto);
    DeviceSettingDTO update(Long id, DeviceSettingDTO dto);
    void delete(Long id);
}
