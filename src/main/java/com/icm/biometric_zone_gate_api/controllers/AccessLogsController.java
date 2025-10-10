package com.icm.biometric_zone_gate_api.controllers;

import com.icm.biometric_zone_gate_api.enums.AccessType;
import com.icm.biometric_zone_gate_api.models.AccessLogsModel;
import com.icm.biometric_zone_gate_api.services.AccessLogsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/access-logs")
@RequiredArgsConstructor
public class AccessLogsController {

    private final AccessLogsService accessLogsService;

    @GetMapping
    public ResponseEntity<List<AccessLogsModel>> getAllLogs() {
        return ResponseEntity.ok(accessLogsService.getAllLogs());
    }

    @GetMapping("/page")
    public ResponseEntity<Page<AccessLogsModel>> getAllLogsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Pageable pageable = (sortBy != null && !sortBy.isEmpty())
                ? PageRequest.of(page, size, direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending())
                : PageRequest.of(page, size);

        return ResponseEntity.ok(accessLogsService.getAllLogs(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccessLogsModel> getLogById(@PathVariable Long id) {
        return accessLogsService.getLogById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AccessLogsModel> createLog(@RequestBody AccessLogsModel log) {
        return ResponseEntity.ok(accessLogsService.createLog(log));
    }

    @PutMapping("/{id}/observation")
    public ResponseEntity<AccessLogsModel> updateObservation(
            @PathVariable Long id,
            @RequestParam String observation) {

        Optional<AccessLogsModel> updatedLog = accessLogsService.updateObservation(id, observation);
        return updatedLog
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLog(@PathVariable Long id) {
        return accessLogsService.deleteLog(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccessLogsModel>> getLogsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(accessLogsService.getLogsByUser(userId));
    }

    @GetMapping("/user/{userId}/page")
    public ResponseEntity<Page<AccessLogsModel>> getLogsByUserPage(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Pageable pageable = (sortBy != null && !sortBy.isEmpty())
                ? PageRequest.of(page, size, direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending())
                : PageRequest.of(page, size);

        return ResponseEntity.ok(accessLogsService.getLogsByUser(userId, pageable));
    }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<AccessLogsModel>> getLogsByDevice(@PathVariable Long deviceId) {
        return ResponseEntity.ok(accessLogsService.getLogsByDevice(deviceId));
    }

    @GetMapping("/device/{deviceId}/page")
    public ResponseEntity<Page<AccessLogsModel>> getLogsByDevicePage(
            @PathVariable Long deviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Pageable pageable = (sortBy != null && !sortBy.isEmpty())
                ? PageRequest.of(page, size, direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending())
                : PageRequest.of(page, size);

        return ResponseEntity.ok(accessLogsService.getLogsByDevice(deviceId, pageable));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<AccessLogsModel>> getLogsByCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(accessLogsService.getLogsByCompany(companyId));
    }

    @GetMapping("/company/{companyId}/page")
    public ResponseEntity<Page<AccessLogsModel>> getLogsByCompanyPage(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Pageable pageable = (sortBy != null && !sortBy.isEmpty())
                ? PageRequest.of(page, size, direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending())
                : PageRequest.of(page, size);

        return ResponseEntity.ok(accessLogsService.getLogsByCompany(companyId, pageable));
    }

    @GetMapping("/action/{action}")
    public ResponseEntity<List<AccessLogsModel>> getLogsByAction(@PathVariable AccessType action) {
        return ResponseEntity.ok(accessLogsService.getLogsByAction(action));
    }

    @GetMapping("/action/{action}/page")
    public ResponseEntity<Page<AccessLogsModel>> getLogsByActionPage(
            @PathVariable AccessType action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Pageable pageable = (sortBy != null && !sortBy.isEmpty())
                ? PageRequest.of(page, size, direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending())
                : PageRequest.of(page, size);

        return ResponseEntity.ok(accessLogsService.getLogsByAction(action, pageable));
    }

    @GetMapping("/device/{deviceId}/count")
    public ResponseEntity<Long> countLogsByDeviceAndDay(
            @PathVariable Long deviceId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        long count = accessLogsService.countLogsByDeviceAndDay(deviceId, date);
        return ResponseEntity.ok(count);
    }
}
