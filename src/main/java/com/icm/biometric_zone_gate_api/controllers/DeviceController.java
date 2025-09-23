package com.icm.biometric_zone_gate_api.controllers;

import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.services.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping
    public ResponseEntity<DeviceModel> createDevice(@RequestBody DeviceModel device) {
        return ResponseEntity.ok(deviceService.createDevice(device));
    }

    @GetMapping
    public ResponseEntity<List<DeviceModel>> getAllDevices() {
        return ResponseEntity.ok(deviceService.getAllDevices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceModel> getDeviceById(@PathVariable Long id) {
        return deviceService.getDeviceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<DeviceModel> getDeviceByName(@PathVariable String name) {
        return deviceService.getDeviceByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceModel> updateDevice(@PathVariable Long id, @RequestBody DeviceModel updatedDevice) {
        return deviceService.updateDevice(id, updatedDevice)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        return deviceService.deleteDevice(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
