package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.models.*;
import com.icm.biometric_zone_gate_api.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class SendUserHandler {

    private final UserRepository userRepository;
    private final DeviceUserRepository deviceUserRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final DeviceRepository deviceRepository;

    public void handleSendUser(JsonNode json, WebSocketSession session) {
        try {
            // Obtener SN desde la sesión
            String sn = (String) session.getAttributes().get("sn");
            if (sn == null || sn.isBlank()) {
                System.err.println("No SN found in session attributes");
                session.sendMessage(new TextMessage("{\"ret\":\"senduser\",\"result\":false,\"reason\":1}"));
                return;
            }

            // Buscar dispositivo
            DeviceModel device = deviceRepository.findBySn(sn)
                    .orElseThrow(() -> new IllegalStateException("Device with SN " + sn + " not found"));
            Long deviceId = device.getId();

            // Leer campos del JSON
            int enrollId = json.path("enrollid").asInt(-1);
            String name = json.path("name").asText(null);
            int backupNum = json.path("backupnum").asInt(-1);
            int admin = json.path("admin").asInt(0);
            String record = json.path("record").asText(null);

            // Validaciones
            if (enrollId <= 0 || name == null || backupNum < 0 || record == null || record.isBlank()) {
                System.err.println("Invalid user info: missing or invalid fields");
                session.sendMessage(new TextMessage("{\"ret\":\"senduser\",\"result\":false,\"reason\":1}"));
                return;
            }

            // Buscar o crear usuario
            UserModel user = userRepository.findByName(name)
                    .orElseGet(() -> {
                        UserModel newUser = new UserModel();
                        newUser.setName(name);
                        newUser.setAdminLevel(admin);
                        newUser.setEnabled(true);
                        newUser.setRole(com.icm.biometric_zone_gate_api.enums.Role.USER);
                        newUser.setCompany(device.getCompany()); // si aplica
                        return userRepository.save(newUser);
                    });

            // Buscar o crear DeviceUser
            DeviceUserModel deviceUser = deviceUserRepository.findByDeviceIdAndEnrollId(deviceId, enrollId)
                    .orElseGet(() -> {
                        DeviceUserModel du = new DeviceUserModel();
                        du.setDevice(device);
                        du.setEnrollId(enrollId);
                        du.setUser(user);
                        du.setAdminLevel(admin);
                        du.setSynced(true);
                        return deviceUserRepository.save(du);
                    });

            // Guardar o actualizar credencial
            UserCredentialModel credential = userCredentialRepository
                    .findByDeviceUserIdAndBackupNum(deviceUser.getId(), backupNum)
                    .orElseGet(() -> {
                        UserCredentialModel c = new UserCredentialModel();
                        c.setDeviceUser(deviceUser);
                        c.setBackupNum(backupNum);
                        return c;
                    });
            credential.setRecord(record);
            userCredentialRepository.save(credential);

            // Determinar tipo de registro
            String recordType = switch (backupNum) {
                case 10 -> "password";
                case 11 -> "rfid_card";
                default -> (backupNum >= 0 && backupNum <= 9) ? "fingerprint" : "unknown";
            };

            // Logs
            System.out.println("Received user info from device:");
            System.out.println(" ├─ enrollid: " + enrollId);
            System.out.println(" ├─ name: " + name);
            System.out.println(" ├─ backupnum: " + backupNum + " (" + recordType + ")");
            System.out.println(" ├─ admin: " + admin);
            System.out.println(" └─ record: " + record);

            // Respuesta exitosa
            String cloudTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String response = String.format(
                    "{\"ret\":\"senduser\",\"result\":true,\"cloudtime\":\"%s\"}",
                    cloudTime
            );
            session.sendMessage(new TextMessage(response));

        } catch (Exception e) {
            e.printStackTrace();
            try {
                session.sendMessage(new TextMessage("{\"ret\":\"senduser\",\"result\":false,\"reason\":1}"));
            } catch (Exception ignored) {}
        }
    }
}
