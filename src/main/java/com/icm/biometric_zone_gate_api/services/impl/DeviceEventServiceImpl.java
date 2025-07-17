package com.icm.biometric_zone_gate_api.services.impl;

import com.icm.biometric_zone_gate_api.dto.GlogEventDTO;
import com.icm.biometric_zone_gate_api.models.AccessLogModel;
import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.repositories.AccessLogRepository;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.services.DeviceEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class DeviceEventServiceImpl implements DeviceEventService {
    private final AccessLogRepository accessLogRepository;
    private final DeviceRepository deviceRepository;

    // NOTE: Verify if devModel is needed in this service
    @Override
    public void processGlogEvent(String devId, String devModel, String token, GlogEventDTO dto) {
        DeviceModel device = deviceRepository.findById(devId)
                .orElseThrow(() -> new RuntimeException("Dispositivo no registrado"));

        if (!device.getToken().equals(token) && token != null) {
            throw new SecurityException("Token inválido para el dispositivo " + devId);
        }

        // Map and save the GlogEventDTO to AccessLogModel
        AccessLogModel log = new AccessLogModel();
        log.setDevice(device);
        log.setUserId(dto.getUserId());
        log.setVerifyMode(dto.getVerifyMode());
        log.setIoMode(dto.getIoMode());
        log.setInOut(dto.getInOut());
        log.setDoorMode(dto.getDoorMode());
        log.setLogPhoto(dto.getLogPhoto());
        log.setTimestamp(LocalDateTime.parse(dto.getTime(), DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

        accessLogRepository.save(log);
    }
}
