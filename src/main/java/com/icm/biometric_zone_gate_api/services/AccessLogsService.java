package com.icm.biometric_zone_gate_api.services;

import com.icm.biometric_zone_gate_api.enums.AccessType;
import com.icm.biometric_zone_gate_api.models.AccessLogsModel;
import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.models.UserModel;
import com.icm.biometric_zone_gate_api.repositories.AccessLogsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccessLogsService {

    private final AccessLogsRepository accessLogsRepository;

    public List<AccessLogsModel> getAllLogs() {
        return accessLogsRepository.findAll();
    }

    public Page<AccessLogsModel> getAllLogs(Pageable pageable) {
        return accessLogsRepository.findAll(pageable);
    }

    public Optional<AccessLogsModel> getLogById(Long id) {
        return accessLogsRepository.findById(id);
    }

    public AccessLogsModel createLog(AccessLogsModel log) {
        return accessLogsRepository.save(log);
    }

    public Optional<AccessLogsModel> updateObservation(Long id, String observation) {
        return accessLogsRepository.findById(id).map(log -> {
            log.setObservation(observation);
            return accessLogsRepository.save(log);
        });
    }

    public boolean deleteLog(Long id) {
        if (accessLogsRepository.existsById(id)) {
            accessLogsRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<AccessLogsModel> getLogsByUser(Long userId) {
        return accessLogsRepository.findByUserId(userId);
    }

    public Page<AccessLogsModel> getLogsByUser(Long userId, Pageable pageable) {
        return accessLogsRepository.findByUserId(userId, pageable);
    }

    public List<AccessLogsModel> getLogsByDevice(Long deviceId) {
        return accessLogsRepository.findByDeviceId(deviceId);
    }

    public Page<AccessLogsModel> getLogsByDevice(Long deviceId, Pageable pageable) {
        return accessLogsRepository.findByDeviceId(deviceId, pageable);
    }

    public List<AccessLogsModel> getLogsByCompany(Long companyId) {
        return accessLogsRepository.findByCompanyId(companyId);
    }

    public Page<AccessLogsModel> getLogsByCompany(Long companyId, Pageable pageable) {
        return accessLogsRepository.findByCompanyId(companyId, pageable);
    }

    public List<AccessLogsModel> getLogsByAction(AccessType action) {
        return accessLogsRepository.findByAction(action);
    }

    public Page<AccessLogsModel> getLogsByAction(AccessType action, Pageable pageable) {
        return accessLogsRepository.findByAction(action, pageable);
    }

    public long countLogsByDeviceAndDay(Long deviceId, LocalDate date) {
        ZonedDateTime startOfDay = date.atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime endOfDay = date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault());
        return accessLogsRepository.countByDeviceAndDay(deviceId, startOfDay, endOfDay);
    }


    public List<AccessLogsModel> getLatest4LogsByDeviceToday(Long deviceId) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime startOfDay = now.toLocalDate().atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime endOfDay = now.toLocalDate().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault());

        PageRequest top4 = PageRequest.of(0, 4, Sort.by(Sort.Direction.DESC, "entryTime"));

        return accessLogsRepository
                .findByDeviceIdAndEntryTimeBetween(deviceId, startOfDay, endOfDay, top4)
                .getContent();
    }

    public Optional<AccessLogsModel> getOpenLogForUserDevice(UserModel user, DeviceModel device) {
        return accessLogsRepository.findOpenLogByUserAndDevice(user.getId(), device.getId());
    }
}
