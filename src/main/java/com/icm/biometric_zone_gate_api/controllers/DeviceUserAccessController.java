package com.icm.biometric_zone_gate_api.controllers;

import com.icm.biometric_zone_gate_api.models.DeviceUserAccessModel;
import com.icm.biometric_zone_gate_api.services.DeviceUserAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/device-user-access")
@RequiredArgsConstructor
public class DeviceUserAccessController {

    private final DeviceUserAccessService deviceUserAccessService;

    // =========================
    // CRUD BÁSICO
    // =========================
    @GetMapping("/{id}")
    public ResponseEntity<DeviceUserAccessModel> getById(@PathVariable Long id) {
        return deviceUserAccessService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<DeviceUserAccessModel>> getAll() {
        return ResponseEntity.ok(deviceUserAccessService.findAll());
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<DeviceUserAccessModel>> getAllPaged(@RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "10") int size,
                                                                   @RequestParam(required = false) String sortBy,
                                                                   @RequestParam(defaultValue = "asc") String direction) {
        Pageable pageable;
        if (sortBy != null && !sortBy.isEmpty()) {
            pageable = PageRequest.of(
                    page,
                    size,
                    direction.equalsIgnoreCase("desc")
                            ? Sort.by(sortBy).descending()
                            : Sort.by(sortBy).ascending()
            );
        } else {
            pageable = PageRequest.of(page, size);
        }

        return ResponseEntity.ok(deviceUserAccessService.findAll(pageable));
    }

    @PostMapping
    public ResponseEntity<DeviceUserAccessModel> create(@RequestBody DeviceUserAccessModel access) {
        return ResponseEntity.ok(deviceUserAccessService.save(access));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceUserAccessModel> update(
            @PathVariable Long id,
            @RequestBody DeviceUserAccessModel updatedAccess) {
        return deviceUserAccessService.update(id, updatedAccess)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boolean deleted = deviceUserAccessService.deleteById(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // =========================
    // FILTROS PERSONALIZADOS
    // =========================

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DeviceUserAccessModel>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(deviceUserAccessService.findByUserId(userId));
    }

    @GetMapping("/user/{userId}/paged")
    public ResponseEntity<Page<DeviceUserAccessModel>> getByUserIdPaged(
            @PathVariable Long userId, Pageable pageable) {
        return ResponseEntity.ok(deviceUserAccessService.findByUserId(userId, pageable));
    }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<DeviceUserAccessModel>> getByDeviceId(@PathVariable Long deviceId) {
        return ResponseEntity.ok(deviceUserAccessService.findByDeviceId(deviceId));
    }

    @GetMapping("/device/{deviceId}/paged")
    public ResponseEntity<Page<DeviceUserAccessModel>> getByDeviceIdPaged(
            @PathVariable Long deviceId, Pageable pageable) {
        return ResponseEntity.ok(deviceUserAccessService.findByDeviceId(deviceId, pageable));
    }

    @GetMapping("/device/{deviceId}/enabled")
    public ResponseEntity<Page<DeviceUserAccessModel>> getByDeviceIdAndEnabledTrue(
            @PathVariable Long deviceId, Pageable pageable) {
        return ResponseEntity.ok(deviceUserAccessService.findByDeviceIdAndEnabledTrue(deviceId, pageable));
    }

    @GetMapping("/group/{groupNumber}")
    public ResponseEntity<List<DeviceUserAccessModel>> getByGroupNumber(@PathVariable Integer groupNumber) {
        return ResponseEntity.ok(deviceUserAccessService.findByGroupNumber(groupNumber));
    }

    @GetMapping("/group/{groupNumber}/paged")
    public ResponseEntity<Page<DeviceUserAccessModel>> getByGroupNumberPaged(
            @PathVariable Integer groupNumber, Pageable pageable) {
        return ResponseEntity.ok(deviceUserAccessService.findByGroupNumber(groupNumber, pageable));
    }

    @GetMapping("/enabled")
    public ResponseEntity<List<DeviceUserAccessModel>> getEnabled() {
        return ResponseEntity.ok(deviceUserAccessService.findByEnabledTrue());
    }

    @GetMapping("/enabled/paged")
    public ResponseEntity<Page<DeviceUserAccessModel>> getEnabledPaged(Pageable pageable) {
        return ResponseEntity.ok(deviceUserAccessService.findByEnabledTrue(pageable));
    }

    @GetMapping("/user/{userId}/device/{deviceId}")
    public ResponseEntity<DeviceUserAccessModel> getByUserAndDevice(
            @PathVariable Long userId, @PathVariable Long deviceId) {
        return deviceUserAccessService.findByUserIdAndDeviceId(userId, deviceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}/device/{deviceId}/enabled")
    public ResponseEntity<DeviceUserAccessModel> getByUserAndDeviceEnabled(
            @PathVariable Long userId, @PathVariable Long deviceId) {
        return deviceUserAccessService.findByUserIdAndDeviceIdAndEnabledTrue(userId, deviceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
