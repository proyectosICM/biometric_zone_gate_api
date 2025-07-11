package com.icm.biometric_zone_gate_api.controllers;

import com.icm.biometric_zone_gate_api.dto.DeviceDTO;
import com.icm.biometric_zone_gate_api.dto.DeviceSettingDTO;
import com.icm.biometric_zone_gate_api.services.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/devices")
public class DeviceController {
    private final DeviceService deviceService;

    // GET /api/v1/devices/{id}
    @GetMapping("/{id}")
    public ResponseEntity<DeviceDTO> getDeviceById(@PathVariable Long id) {
        return deviceService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/v1/devices
    @PostMapping
    public ResponseEntity<DeviceDTO> createDevice(@RequestBody DeviceDTO deviceDTO) {
        DeviceDTO createdDevice = deviceService.save(deviceDTO);
        if (createdDevice == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDevice);
    }

    // PUT /api/v1/devices/{id}
    @PutMapping("/{id}")
    public ResponseEntity<DeviceDTO> updateDevice(@PathVariable Long id, @RequestBody DeviceDTO deviceDTO) {
        deviceDTO.setId(id);
        try {
            DeviceDTO updatedDevice = deviceService.update(deviceDTO);
            if (updatedDevice == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updatedDevice);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE /api/v1/devices/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        try {
            deviceService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // POST /api/v1/devices/{id}/settings/send
    @PostMapping("/{id}/settings/send")
    public ResponseEntity<String> sendSettingToDevice(
            @PathVariable Long id,
            @RequestBody DeviceSettingDTO settingDTO) {

        String response = deviceService.sendDeviceSetting(id, settingDTO);
        return ResponseEntity.ok(response);
    }
}
