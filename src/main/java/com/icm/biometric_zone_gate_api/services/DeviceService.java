package com.icm.biometric_zone_gate_api.services;

import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public DeviceModel createDevice(DeviceModel device) {
        return deviceRepository.save(device);
    }

    public List<DeviceModel> getAllDevices() {
        return deviceRepository.findAll();
    }

    public Optional<DeviceModel> getDeviceById(Long id) {
        return deviceRepository.findById(id);
    }

    public Optional<DeviceModel> getDeviceByName(String name) {
        return Optional.ofNullable(deviceRepository.findByName(name));
    }

    public Optional<DeviceModel> updateDevice(Long id, DeviceModel updatedDevice) {
        return deviceRepository.findById(id).map(device -> {
            device.setName(updatedDevice.getName());
            device.setHost(updatedDevice.getHost());
            device.setPort(updatedDevice.getPort());
            device.setPushEnabled(updatedDevice.isPushEnabled());
            device.setLanguage(updatedDevice.getLanguage());
            device.setVolume(updatedDevice.getVolume());
            device.setAntiPassback(updatedDevice.isAntiPassback());
            device.setSleepTimeMinutes(updatedDevice.getSleepTimeMinutes());
            device.setVerificationMode(updatedDevice.getVerificationMode());
            device.setOldAdminPassword(updatedDevice.getOldAdminPassword());
            device.setNewAdminPassword(updatedDevice.getNewAdminPassword());
            return deviceRepository.save(device);
        });
    }

    public boolean deleteDevice(Long id) {
        if (deviceRepository.existsById(id)) {
            deviceRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
