package com.icm.biometric_zone_gate_api.services;

import com.icm.biometric_zone_gate_api.dto.DeviceUserAccessDTO;
import com.icm.biometric_zone_gate_api.models.DeviceUserAccessModel;
import com.icm.biometric_zone_gate_api.repositories.DeviceUserAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.icm.biometric_zone_gate_api.mappers.DeviceUserAccessMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceUserAccessService {
    private final DeviceUserAccessRepository deviceUserAccessRepository;

    public Optional<DeviceUserAccessModel> findById(Long id) {
        return deviceUserAccessRepository.findById(id);
    }

    public List<DeviceUserAccessDTO> findAll() {
        List<DeviceUserAccessModel> entities = deviceUserAccessRepository.findAll();
        return entities.stream()
                .map(DeviceUserAccessMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Page<DeviceUserAccessDTO> findAll(Pageable pageable) {
        Page<DeviceUserAccessModel> entities = deviceUserAccessRepository.findAll(pageable);

        // Mapear de Entity a DTO
        return new PageImpl<>(
                entities.stream()
                        .map(DeviceUserAccessMapper::toDTO)
                        .collect(Collectors.toList()),
                pageable,
                entities.getTotalElements()
        );
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

    public Optional<DeviceUserAccessDTO> findByUserIdAndDeviceId(Long userId, Long deviceId) {
        return deviceUserAccessRepository.findByUserIdAndDeviceId(userId, deviceId)
                .map(DeviceUserAccessMapper::toDTO);
    }

    public Optional<DeviceUserAccessDTO> findByUserIdAndDeviceIdAndEnabledTrue(Long userId, Long deviceId) {
        return deviceUserAccessRepository.findByUserIdAndDeviceIdAndEnabledTrue(userId, deviceId)
                .map(DeviceUserAccessMapper::toDTO);
    }


    public List<DeviceUserAccessDTO> findByUserId(Long userId) {
        List<DeviceUserAccessModel> entities = deviceUserAccessRepository.findByUserId(userId);
        return entities.stream()
                .map(DeviceUserAccessMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Page<DeviceUserAccessDTO> findByUserId(Long userId, Pageable pageable) {
        Page<DeviceUserAccessModel> page = deviceUserAccessRepository.findByUserId(userId, pageable);
        return page.map(DeviceUserAccessMapper::toDTO);
    }

    public List<DeviceUserAccessDTO> findByDeviceId(Long deviceId) {
        List<DeviceUserAccessModel> entities = deviceUserAccessRepository.findByDeviceId(deviceId);
        return entities.stream()
                .map(DeviceUserAccessMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Page<DeviceUserAccessDTO> findByDeviceId(Long deviceId, Pageable pageable) {
        Page<DeviceUserAccessModel> page = deviceUserAccessRepository.findByDeviceId(deviceId, pageable);
        return page.map(DeviceUserAccessMapper::toDTO);
    }

    public Page<DeviceUserAccessDTO> findByDeviceIdAndEnabledTrue(Long deviceId, Pageable pageable) {
        Page<DeviceUserAccessModel> page = deviceUserAccessRepository.findByDeviceIdAndEnabledTrue(deviceId, pageable);
        return page.map(DeviceUserAccessMapper::toDTO);
    }

    public List<DeviceUserAccessDTO> findByGroupNumber(Integer groupNumber) {
        List<DeviceUserAccessModel> entities = deviceUserAccessRepository.findByGroupNumber(groupNumber);
        return entities.stream()
                .map(DeviceUserAccessMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Page<DeviceUserAccessDTO> findByGroupNumber(Integer groupNumber, Pageable pageable) {
        Page<DeviceUserAccessModel> page = deviceUserAccessRepository.findByGroupNumber(groupNumber, pageable);
        return page.map(DeviceUserAccessMapper::toDTO);
    }

    public List<DeviceUserAccessDTO> findByEnabledTrue() {
        List<DeviceUserAccessModel> entities = deviceUserAccessRepository.findByEnabledTrue();
        return entities.stream()
                .map(DeviceUserAccessMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Page<DeviceUserAccessDTO> findByEnabledTrue(Pageable pageable) {
        Page<DeviceUserAccessModel> page = deviceUserAccessRepository.findByEnabledTrue(pageable);
        return page.map(DeviceUserAccessMapper::toDTO);
    }
}
