package com.icm.biometric_zone_gate_api.controllers;

import com.icm.biometric_zone_gate_api.enums.AccessType;
import com.icm.biometric_zone_gate_api.models.AccessLogsModel;
import com.icm.biometric_zone_gate_api.services.AccessLogsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/access-logs")
@RequiredArgsConstructor
public class AccessLogsController {

    private final AccessLogsService accessLogsService;

    @PostMapping
    public ResponseEntity<AccessLogsModel> createLog(@RequestBody AccessLogsModel log) {
        return ResponseEntity.ok(accessLogsService.createLog(log));
    }

    @GetMapping
    public ResponseEntity<List<AccessLogsModel>> getAllLogs() {
        return ResponseEntity.ok(accessLogsService.getAllLogs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccessLogsModel> getLogById(@PathVariable Long id) {
        return accessLogsService.getLogById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccessLogsModel>> getLogsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(accessLogsService.getLogsByUser(userId));
    }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<AccessLogsModel>> getLogsByDevice(@PathVariable Long deviceId) {
        return ResponseEntity.ok(accessLogsService.getLogsByDevice(deviceId));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<AccessLogsModel>> getLogsByCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(accessLogsService.getLogsByCompany(companyId));
    }

    @GetMapping("/action/{action}")
    public ResponseEntity<List<AccessLogsModel>> getLogsByAction(@PathVariable AccessType action) {
        return ResponseEntity.ok(accessLogsService.getLogsByAction(action));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLog(@PathVariable Long id) {
        return accessLogsService.deleteLog(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
