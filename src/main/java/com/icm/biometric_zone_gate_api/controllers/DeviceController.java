package com.icm.biometric_zone_gate_api.controllers;

import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.models.UserModel;
import com.icm.biometric_zone_gate_api.services.DeviceService;
import com.icm.biometric_zone_gate_api.websocket.DeviceSessionManager;
import com.icm.biometric_zone_gate_api.websocket.commands.GetUserInfoCommandSender;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final GetUserInfoCommandSender getUserInfoCommandSender;
    private final DeviceSessionManager deviceSessionManager;

    @PostMapping
    public ResponseEntity<DeviceModel> createDevice(@RequestBody DeviceModel device) {
        return ResponseEntity.ok(deviceService.createDevice(device));
    }

    @GetMapping
    public ResponseEntity<List<DeviceModel>> getAllDevices() {
        return ResponseEntity.ok(deviceService.getAllDevices());
    }

    @GetMapping("/page")
    public ResponseEntity<Page<DeviceModel>> getAllDevices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
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

        return ResponseEntity.ok(deviceService.getAllDevices(pageable));
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

    @GetMapping("/{sn}/user/{enrollId}/{backupNum}")
    public ResponseEntity<String> getUserInfo(
            @PathVariable String sn,
            @PathVariable int enrollId,
            @PathVariable int backupNum) {

        WebSocketSession session = deviceSessionManager.getSessionBySn(sn);
        if (session == null || !session.isOpen()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Dispositivo no conectado");
        }

        getUserInfoCommandSender.sendGetUserInfoCommand(session, enrollId, backupNum);
        return ResponseEntity.ok("Comando enviado correctamente");
    }

    /*
    @GetMapping("/{id}/sync-users")
    public ResponseEntity<String> syncUsers(@PathVariable Long id) {
        try {
            deviceService.syncUsersFromDevice(id);
            return ResponseEntity.ok("User list requested from device.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/push-user")
    public ResponseEntity<String> pushUser(@PathVariable Long id, @RequestBody UserModel user) {
        deviceService.pushUserToDevice(id, user);
        return ResponseEntity.ok("User sent to device.");
    }

    @DeleteMapping("/{id}/users/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id, @PathVariable Long userId) {
        deviceService.removeUserFromDevice(id, userId);
        return ResponseEntity.ok("User deleted from device.");
    }

    @DeleteMapping("/{id}/users")
    public ResponseEntity<String> clearAll(@PathVariable Long id) {
        deviceService.clearAllDeviceUsers(id);
        return ResponseEntity.ok("All users cleared from device.");
    }
*/
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<DeviceModel>> listByCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(deviceService.listByCompany(companyId));
    }

    @GetMapping("/company/{companyId}/page")
    public ResponseEntity<Page<DeviceModel>> listByCompanyPaginated(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
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

        return ResponseEntity.ok(deviceService.listByCompanyPaginated(companyId, pageable));
    }

    @PostMapping("/{deviceId}/get-username/{enrollId}")
    public ResponseEntity<?> getUserName(
            @PathVariable Long deviceId,
            @PathVariable int enrollId
    ) {
        try {
            deviceService.requestUserName(deviceId, enrollId);
            return ResponseEntity.ok("Comando GET USER NAME enviado al dispositivo.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/initialize-system")
    public ResponseEntity<String> initializeSystem(@PathVariable Long id) {
        deviceService.initializeSystem(id);
        return ResponseEntity.ok("Comando INIT SYSTEM enviado para el dispositivo con ID " + id);
    }

    @PostMapping("/{id}/reboot")
    public ResponseEntity<String> rebootDevice(@PathVariable Long id) {
        deviceService.rebootDevice(id);
        return ResponseEntity.ok("Comando REBOOT enviado para el dispositivo con ID " + id);
    }

    @PostMapping("/{id}/clean-admins")
    public ResponseEntity<String> cleanAdmins(@PathVariable Long id) {
        deviceService.cleanAdmins(id);
        return ResponseEntity.ok("Comando CLEAN ADMIN enviado para el dispositivo con ID " + id);
    }

    @PostMapping("/{id}/settime")
    public ResponseEntity<String> syncDeviceTimeNow(@PathVariable Long id) {
        deviceService.syncDeviceTimeNow(id);
        return ResponseEntity.ok("⏰ Comando SETTIME con hora actual enviado al dispositivo con ID " + id);
    }

    // 🕓 Sincroniza con hora personalizada
    @PostMapping("/{id}/settime/custom")
    public ResponseEntity<String> syncDeviceTimeCustom(
            @PathVariable Long id,
            @RequestParam("datetime")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime customTime) {

        deviceService.syncDeviceTimeCustom(id, customTime);
        return ResponseEntity.ok("🕓 Comando SETTIME con hora personalizada enviado al dispositivo con ID " + id);
    }

    @PostMapping("/{id}/open-door")
    public ResponseEntity<String> openDoor(@PathVariable Long id) {
        deviceService.openDoor(id);
        return ResponseEntity.ok("🔓 Comando OPENDOOR enviado al dispositivo con ID " + id);
    }

    @PostMapping("/{id}/get-devinfo")
    public ResponseEntity<String> getDeviceInfo(@PathVariable Long id) {
        deviceService.requestDeviceInfo(id);
        return ResponseEntity.ok("📡 Comando GETDEVINFO enviado al dispositivo con ID " + id);
    }

    @PostMapping("/{id}/clean-logs")
    public ResponseEntity<String> cleanDeviceLogs(@PathVariable Long id) {
        deviceService.cleanDeviceLogs(id);
        return ResponseEntity.ok("🧹 Comando CLEANLOG enviado al dispositivo con ID " + id);
    }

    @PostMapping("/{deviceId}/getnewlog")
    public ResponseEntity<String> requestNewLogs(
            @PathVariable Long deviceId,
            @RequestParam(defaultValue = "true") boolean start) {
        deviceService.requestNewLogs(deviceId, start);
        return ResponseEntity.ok("Comando GETNEWLOG enviado (start=" + start + ")");
    }

    @PostMapping("/{deviceId}/getalllog")
    public ResponseEntity<String> requestAllLogs(
            @PathVariable Long deviceId,
            @RequestParam(defaultValue = "true") boolean start) {
        deviceService.requestAllLogs(deviceId, start);
        return ResponseEntity.ok("Comando GETNEWLOG enviado (start=" + start + ")");
    }

}
