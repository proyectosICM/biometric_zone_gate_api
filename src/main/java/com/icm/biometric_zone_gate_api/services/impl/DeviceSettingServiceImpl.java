package com.icm.biometric_zone_gate_api.services.impl;

import com.icm.biometric_zone_gate_api.dto.DeviceSettingDTO;
import com.icm.biometric_zone_gate_api.mappers.DeviceSettingMapper;
import com.icm.biometric_zone_gate_api.models.DeviceSettingModel;
import com.icm.biometric_zone_gate_api.repositories.DeviceSettingRepository;
import com.icm.biometric_zone_gate_api.services.DeviceSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeviceSettingServiceImpl implements DeviceSettingService {
    private final DeviceSettingRepository deviceSettingRepository;

    @Override
    public Optional<DeviceSettingDTO> findById(Long id) {
        return deviceSettingRepository.findById(id)
                .map(DeviceSettingMapper::toDTO);
    }

    @Override
    public DeviceSettingDTO save(DeviceSettingDTO dto) {
        DeviceSettingModel entity = DeviceSettingMapper.toEntity(dto);
        DeviceSettingModel saved = deviceSettingRepository.save(entity);
        return DeviceSettingMapper.toDTO(saved);
    }

    @Override
    public DeviceSettingDTO update(Long id, DeviceSettingDTO dto) {
        return deviceSettingRepository.findById(id)
                .map(existing -> {
                    existing.setDeviceName(dto.getDeviceName());
                    existing.setServerHost(dto.getServerHost());
                    existing.setServerPort(dto.getServerPort());
                    existing.setIdioma(dto.getIdioma());
                    existing.setVolumen(dto.getVolumen());
                    existing.setAntiPass(dto.getAntiPass());
                    existing.setDoorOpenDelay(dto.getDoorOpenDelay());
                    existing.setVerificationMode(dto.getVerificationMode());
                    existing.setOldPwd(dto.getOldPwd());
                    existing.setNewPwd(dto.getNewPwd());
                    return DeviceSettingMapper.toDTO(deviceSettingRepository.save(existing));
                })
                .orElseThrow(() -> new RuntimeException("Device setting not found with id: " + id));
    }

    @Override
    public void delete(Long id) {
        if (!deviceSettingRepository.existsById(id)) {
            throw new RuntimeException("Device setting not found with id: " + id);
        }
        deviceSettingRepository.deleteById(id);
    }
}
