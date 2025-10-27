package com.icm.biometric_zone_gate_api.controllers;

import com.icm.biometric_zone_gate_api.dto.DeviceUserAccessDTO;
import com.icm.biometric_zone_gate_api.mappers.DeviceUserAccessMapper;
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
    public ResponseEntity<DeviceUserAccessDTO> getById(@PathVariable Long id) {
        return deviceUserAccessService.findById(id)
                .map(DeviceUserAccessMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<DeviceUserAccessDTO>> getAll() {
        return ResponseEntity.ok(deviceUserAccessService.findAll());
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<DeviceUserAccessDTO>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Pageable pageable = (sortBy != null && !sortBy.isEmpty()) ?
                PageRequest.of(page, size, direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending()) :
                PageRequest.of(page, size);

        return ResponseEntity.ok(deviceUserAccessService.findAll(pageable));
    }

    @PostMapping
    public ResponseEntity<DeviceUserAccessDTO> create(@RequestBody DeviceUserAccessDTO dto) {
        DeviceUserAccessDTO saved = deviceUserAccessService.save(dto);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceUserAccessDTO> update(@PathVariable Long id,
                                                      @RequestBody DeviceUserAccessDTO dto) {
        DeviceUserAccessModel updatedEntity = DeviceUserAccessMapper.toEntity(dto);
        return deviceUserAccessService.update(id, updatedEntity)
                .map(DeviceUserAccessMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boolean deleted = deviceUserAccessService.deleteById(id);
        return deleted ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    // =========================
    // FILTROS PERSONALIZADOS
    // =========================
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DeviceUserAccessDTO>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(deviceUserAccessService.findByUserId(userId));
    }

    @GetMapping("/user/{userId}/paged")
    public ResponseEntity<Page<DeviceUserAccessDTO>> getByUserIdPaged(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Pageable pageable = (sortBy != null && !sortBy.isEmpty()) ?
                PageRequest.of(page, size, direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending()) :
                PageRequest.of(page, size);

        return ResponseEntity.ok(deviceUserAccessService.findByUserId(userId, pageable));
    }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<DeviceUserAccessDTO>> getByDeviceId(@PathVariable Long deviceId) {
        return ResponseEntity.ok(deviceUserAccessService.findByDeviceId(deviceId));
    }

    @GetMapping("/device/{deviceId}/paged")
    public ResponseEntity<Page<DeviceUserAccessDTO>> getByDeviceIdPaged(
            @PathVariable Long deviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Pageable pageable = (sortBy != null && !sortBy.isEmpty()) ?
                PageRequest.of(page, size, direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending()) :
                PageRequest.of(page, size);

        return ResponseEntity.ok(deviceUserAccessService.findByDeviceId(deviceId, pageable));
    }

    @GetMapping("/device/{deviceId}/enabled")
    public ResponseEntity<Page<DeviceUserAccessDTO>> getByDeviceIdAndEnabledTrue(
            @PathVariable Long deviceId, Pageable pageable) {
        return ResponseEntity.ok(deviceUserAccessService.findByDeviceIdAndEnabledTrue(deviceId, pageable));
    }

    @GetMapping("/enabled")
    public ResponseEntity<List<DeviceUserAccessDTO>> getEnabled() {
        return ResponseEntity.ok(deviceUserAccessService.findByEnabledTrue());
    }

    @GetMapping("/enabled/paged")
    public ResponseEntity<Page<DeviceUserAccessDTO>> getEnabledPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Pageable pageable = (sortBy != null && !sortBy.isEmpty()) ?
                PageRequest.of(page, size, direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending()) :
                PageRequest.of(page, size);

        return ResponseEntity.ok(deviceUserAccessService.findByEnabledTrue(pageable));
    }

    @GetMapping("/user/{userId}/device/{deviceId}")
    public ResponseEntity<DeviceUserAccessDTO> getByUserAndDevice(
            @PathVariable Long userId,
            @PathVariable Long deviceId) {
        return deviceUserAccessService.findByUserIdAndDeviceId(userId, deviceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}/device/{deviceId}/enabled")
    public ResponseEntity<DeviceUserAccessDTO> getByUserAndDeviceEnabled(
            @PathVariable Long userId,
            @PathVariable Long deviceId) {
        return deviceUserAccessService.findByUserIdAndDeviceIdAndEnabledTrue(userId, deviceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/clean-users")
    public ResponseEntity<String> cleanUsers(@PathVariable Long id) {
        deviceUserAccessService.cleanDeviceUsersBySn(id);
        return ResponseEntity.ok("Comando CLEAN USER enviado y permisos eliminados para el dispositivo " + id);
    }
}
