package com.icm.biometric_zone_gate_api.services;

import com.icm.biometric_zone_gate_api.dto.GlogEventDTO;

public interface DeviceEventService {
    void processGlogEvent(String devId, String devModel, String token, GlogEventDTO dto);
}
