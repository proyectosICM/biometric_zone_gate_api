package com.icm.biometric_zone_gate_api.services;

import com.icm.biometric_zone_gate_api.config.DeviceConnectionManager;
import com.icm.biometric_zone_gate_api.enums.DeviceStatus;
import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.models.UserModel;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.websocket.DeviceWebSocketClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceConnectionManager connectionManager;

    public DeviceModel createDevice(DeviceModel device) {
        return deviceRepository.save(device);
    }

    public List<DeviceModel> getAllDevices() {
        return deviceRepository.findAll();
    }

    public Page<DeviceModel> getAllDevices(Pageable pageable) {
        return deviceRepository.findAll(pageable);
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
            device.setVerificationMode(updatedDevice.getVerificationMode());
            device.setCompany(updatedDevice.getCompany());
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

    public Optional<DeviceModel> getDeviceBySn(String sn) {
        return deviceRepository.findBySn(sn);
    }

    public DeviceModel updateDeviceStatus(Long deviceId, DeviceStatus newStatus) {
        return deviceRepository.findById(deviceId).map(device -> {
            device.setStatus(newStatus);
            return deviceRepository.save(device);
        }).orElseThrow(() -> new RuntimeException("Device not found with id: " + deviceId));
    }

    public List<DeviceModel> listByCompany(Long companyId) {
        return deviceRepository.findByCompanyId(companyId);
    }

    public Page<DeviceModel> listByCompanyPaginated(Long companyId, Pageable pageable) {
        return deviceRepository.findByCompanyId(companyId, pageable);
    }

    public void syncUsersFromDevice(Long deviceId) {
        DeviceWebSocketClient client = connectionManager.getClient(deviceId);
        if (client == null) {
            throw new IllegalStateException("Device not connected or not found.");
        }

        client.requestUserList();
    }

    public void pushUserToDevice(Long deviceId, UserModel user) {
        DeviceWebSocketClient client = connectionManager.getClient(deviceId);
        if (client == null) throw new IllegalStateException("Device not connected.");
        client.sendUserToDevice(user);
    }

    public void removeUserFromDevice(Long deviceId, Long userId) {
        DeviceWebSocketClient client = connectionManager.getClient(deviceId);
        if (client == null) throw new IllegalStateException("Device not connected.");
        client.deleteUserFromDevice(userId);
    }

    public void clearAllDeviceUsers(Long deviceId) {
        DeviceWebSocketClient client = connectionManager.getClient(deviceId);
        if (client == null) throw new IllegalStateException("Device not connected.");
        client.clearAllUsers();
    }
}
