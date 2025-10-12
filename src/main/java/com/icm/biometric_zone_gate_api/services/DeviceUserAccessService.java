package com.icm.biometric_zone_gate_api.services;

import com.icm.biometric_zone_gate_api.models.DeviceUserAccessModel;
import com.icm.biometric_zone_gate_api.repositories.DeviceUserAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeviceUserAccessService {
    private final DeviceUserAccessRepository deviceUserAccessRepository;

    public Optional<DeviceUserAccessModel> findById(Long id) {
        return deviceUserAccessRepository.findById(id);
    }

    public List<DeviceUserAccessModel> findAll() {
        return deviceUserAccessRepository.findAll();
    }

    public Page<DeviceUserAccessModel> findAll(Pageable pageable) {
        return deviceUserAccessRepository.findAll(pageable);
    }

    public DeviceUserAccessModel save(DeviceUserAccessModel access) {
        return deviceUserAccessRepository.save(access);
    }

    public Optional<DeviceUserAccessModel> update(Long id, DeviceUserAccessModel updatedAccess) {
        return deviceUserAccessRepository.findById(id).map(existing -> {
            existing.setUser(updatedAccess.getUser());
            existing.setDevice(updatedAccess.getDevice());
            existing.setWeekZone(updatedAccess.getWeekZone());
            existing.setGroupNumber(updatedAccess.getGroupNumber());
            existing.setStartTime(updatedAccess.getStartTime());
            existing.setEndTime(updatedAccess.getEndTime());
            existing.setEnabled(updatedAccess.getEnabled());
            return deviceUserAccessRepository.save(existing);
        });
    }

    public boolean deleteById(Long id) {
        if (deviceUserAccessRepository.existsById(id)) {
            deviceUserAccessRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<DeviceUserAccessModel> findByUserIdAndDeviceId(Long userId, Long deviceId) {
        return deviceUserAccessRepository.findByUserIdAndDeviceId(userId, deviceId);
    }

    public Optional<DeviceUserAccessModel> findByUserIdAndDeviceIdAndEnabledTrue(Long userId, Long deviceId) {
        return deviceUserAccessRepository.findByUserIdAndDeviceIdAndEnabledTrue(userId, deviceId);
    }

    public List<DeviceUserAccessModel> findByUserId(Long userId) {
        return deviceUserAccessRepository.findByUserId(userId);
    }

    public Page<DeviceUserAccessModel> findByUserId(Long userId, Pageable pageable) {
        return deviceUserAccessRepository.findByUserId(userId, pageable);
    }

    public List<DeviceUserAccessModel> findByDeviceId(Long deviceId) {
        return deviceUserAccessRepository.findByDeviceId(deviceId);
    }

    public Page<DeviceUserAccessModel> findByDeviceId(Long deviceId, Pageable pageable) {
        return deviceUserAccessRepository.findByDeviceId(deviceId, pageable);
    }

    public Page<DeviceUserAccessModel> findByDeviceIdAndEnabledTrue(Long deviceId, Pageable pageable) {
        return deviceUserAccessRepository.findByDeviceIdAndEnabledTrue(deviceId, pageable);
    }

    public List<DeviceUserAccessModel> findByGroupNumber(Integer groupNumber) {
        return deviceUserAccessRepository.findByGroupNumber(groupNumber);
    }

    public Page<DeviceUserAccessModel> findByGroupNumber(Integer groupNumber, Pageable pageable) {
        return deviceUserAccessRepository.findByGroupNumber(groupNumber, pageable);
    }

    public List<DeviceUserAccessModel> findByEnabledTrue() {
        return deviceUserAccessRepository.findByEnabledTrue();
    }

    public Page<DeviceUserAccessModel> findByEnabledTrue(Pageable pageable) {
        return deviceUserAccessRepository.findByEnabledTrue(pageable);
    }
}
