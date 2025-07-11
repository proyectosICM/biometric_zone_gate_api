package com.icm.biometric_zone_gate_api.controllers;

import com.icm.biometric_zone_gate_api.dto.DeviceSettingDTO;
import com.icm.biometric_zone_gate_api.services.DeviceSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/device-settings")
public class DeviceSettingController {
    private final DeviceSettingService deviceSettingService;

    // GET /api/v1/device-settings/{id}
    @GetMapping("/{id}")
    public ResponseEntity<DeviceSettingDTO> getDeviceSettingById(@PathVariable Long id) {
        return deviceSettingService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/v1/device-settings
    @PostMapping
    public ResponseEntity<DeviceSettingDTO> saveDeviceSetting(@RequestBody DeviceSettingDTO dto) {
        DeviceSettingDTO savedDeviceSetting = deviceSettingService.save(dto);
        if (savedDeviceSetting == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(savedDeviceSetting);
    }

    // PUT /api/v1/device-settings/{id}
    @PutMapping("/{id}")
    public ResponseEntity<DeviceSettingDTO> updateDeviceSetting(@PathVariable Long id, @RequestBody DeviceSettingDTO dto) {
        try {
            DeviceSettingDTO updatedDeviceSetting = deviceSettingService.update(id, dto);
            if (updatedDeviceSetting == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updatedDeviceSetting);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE /api/v1/device-settings/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deviceSettingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
