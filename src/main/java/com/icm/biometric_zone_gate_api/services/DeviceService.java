package com.icm.biometric_zone_gate_api.services;

import com.icm.biometric_zone_gate_api.dto.DeviceDTO;
import com.icm.biometric_zone_gate_api.dto.DeviceSettingDTO;

import java.util.Optional;

public interface DeviceService {
    Optional<DeviceDTO> findById(Long id);
    DeviceDTO save(DeviceDTO deviceDTO);
    DeviceDTO update(DeviceDTO deviceDTO);
    void deleteById(Long id);
    String sendDeviceSetting(Long deviceId, DeviceSettingDTO settingDTO);
}
