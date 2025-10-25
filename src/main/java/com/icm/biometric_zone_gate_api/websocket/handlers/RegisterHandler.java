package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.enums.DeviceStatus;
import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.services.DeviceService;
import com.icm.biometric_zone_gate_api.websocket.DeviceSessionManager;
import com.icm.biometric_zone_gate_api.websocket.commands.GetNewLogCommandSender;
import com.icm.biometric_zone_gate_api.websocket.commands.GetUserListCommandSender;
import com.icm.biometric_zone_gate_api.websocket.utils.DeviceCommandScheduler;
import com.icm.biometric_zone_gate_api.websocket.utils.DeviceValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RegisterHandler {

    private final DeviceService deviceService;
    private final GetUserListCommandSender getUserListCommandSender;
    private final DeviceSessionManager deviceSessionManager;
    private final GetNewLogCommandSender getNewLogCommandSender;
    private final DeviceCommandScheduler deviceCommandScheduler;

    private static final int MINUTES_BETWEEN_SYNC = 10;

    public void handleRegister(JsonNode json, WebSocketSession session) {
        try {
            String sn = json.path("sn").asText(null);
            if (sn == null || sn.isEmpty()) {
                System.err.println("Invalid registration: missing SN");
                session.sendMessage(new TextMessage("{\"ret\":\"reg\", \"result\":false, \"reason\":1}"));
                return;
            }

            JsonNode devinfo = json.path("devinfo");
            if (!DeviceValidator.validateDevInfo(devinfo)) {
                System.err.println("Invalid registration: incomplete or incorrect devinfo");
                session.sendMessage(new TextMessage("{\"ret\":\"reg\", \"result\":false, \"reason\":1}"));
                return;
            }

            Optional<DeviceModel> existingDeviceOpt = deviceService.getDeviceBySn(sn);
            if (existingDeviceOpt.isPresent()) {
                DeviceModel device = existingDeviceOpt.get();
                deviceService.updateDeviceStatus(device.getId(), DeviceStatus.CONNECTED);
                session.getAttributes().put("sn", sn);
                deviceSessionManager.registerSession(sn, session);

                System.out.println("Existing device marked as CONNECTED: " + sn);

                // ✅ PRIMERO RESPONDEMOS AL REG
                String cloudTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                String response = String.format("{\"ret\":\"reg\",\"result\":true,\"cloudtime\":\"%s\"}", cloudTime);
                session.sendMessage(new TextMessage(response));

                System.out.println("Registro del dispositivo exitoso: " + sn);

                // ==============================
                // 🔄  SINCRONIZACIÓN CONTROLADA
                // ==============================
                LocalDateTime now = LocalDateTime.now();
                int minute = now.getMinute();
                boolean inSyncWindow = (minute % 10 <= 2);

                if (inSyncWindow) {
                    ZonedDateTime lastSync = device.getLastUserSync();
                    boolean mustSync = (lastSync == null) ||
                            Duration.between(lastSync, now.atZone(ZoneId.systemDefault())).toMinutes() >= MINUTES_BETWEEN_SYNC;

                    if (mustSync) {
                        System.out.println("🟢 Dentro de ventana de sincronización → Enviando GETUSERLIST...");
                        deviceCommandScheduler.schedule(() ->
                                getUserListCommandSender.sendGetUserListCommand(session, true), 500
                        );

                        device.setLastUserSync(ZonedDateTime.now());
                        deviceService.createDevice(device);
                    } else {
                        System.out.println("⏸️ Dentro de ventana, pero sincronización reciente (<10min) → NO se envía.");
                    }
                } else {
                    System.out.println("⛔ Fuera de ventana de sincronización → NO se envía GETUSERLIST.");
                }

            } else {
                System.out.println("⛔ Dispositivo no registrado en BD → NO se sincroniza.");
                // Igual respondemos REG OK para que no se quede colgado
                String cloudTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                session.sendMessage(new TextMessage(String.format("{\"ret\":\"reg\",\"result\":true,\"cloudtime\":\"%s\"}", cloudTime)));
                return;
            }

            // --- GETNEWLOG SIEMPRE ---
            deviceCommandScheduler.schedule(() -> {
                getNewLogCommandSender.sendGetNewLogCommand(session, true);
                System.out.println("Comando GETNEWLOG inicial enviado automáticamente al dispositivo " + sn);
            }, 1000);

        } catch (Exception e) {
            e.printStackTrace();
            try {
                session.sendMessage(new TextMessage("{\"ret\":\"reg\", \"result\":false, \"reason\":1}"));
            } catch (Exception ignored) {}
        }
    }
}
