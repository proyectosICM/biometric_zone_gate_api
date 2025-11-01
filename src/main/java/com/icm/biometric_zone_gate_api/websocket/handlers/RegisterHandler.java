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

import java.time.*;
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
                session.sendMessage(new TextMessage("{\"ret\":\"reg\",\"result\":false,\"reason\":\"did not reg\"}"));
                System.out.println("Sin SN");
                return;
            }

            JsonNode devinfo = json.path("devinfo");
            if (!DeviceValidator.validateDevInfo(devinfo)) {
                session.sendMessage(new TextMessage("{\"ret\":\"reg\",\"result\":false,\"reason\":\"did not reg\"}"));
                return;
            }

            Optional<DeviceModel> existingDeviceOpt = deviceService.getDeviceBySn(sn);

            if (existingDeviceOpt.isPresent()) {
                DeviceModel device = existingDeviceOpt.get();
                deviceService.updateDeviceStatus(device.getId(), DeviceStatus.CONNECTED);
                session.getAttributes().put("sn", sn);
                deviceSessionManager.registerSession(sn, session);

                String cloudTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                session.sendMessage(new TextMessage(
                        String.format("{\"ret\":\"reg\",\"result\":true,\"cloudtime\":\"%s\",\"nosenduser\":true}", cloudTime)
                ));

                // LUEGO evaluamos si se sincroniza
                LocalDateTime now = LocalDateTime.now();
                int minute = now.getMinute();
                boolean inSyncWindow = (minute % 10 <= 8);

                // ✅ Comparación SIEMPRE en UTC con Instant
                Instant nowUtc  = Instant.now();
                ZonedDateTime lastZ = device.getLastUserSync();            // puede venir con cualquier zona
                Instant lastUtc = (lastZ == null) ? null : lastZ.toInstant();

                boolean mustSync;
                if (lastUtc == null) {
                    mustSync = true;
                } else {
                    long deltaMin = Duration.between(lastUtc, nowUtc).toMinutes();
                    // tolera skew: si last está "en el futuro" más de 1 minuto, fuerza sync
                    mustSync = (deltaMin >= MINUTES_BETWEEN_SYNC) || (deltaMin < -1);
                }

                if (inSyncWindow && mustSync ) {
                    deviceCommandScheduler.schedule(() -> {
                        try {
                            if (session != null && session.isOpen()) {
                                getUserListCommandSender.sendGetUserListCommand(session, true);

                                // ✅ Persistir SIEMPRE en UTC
                                Instant ts = Instant.now();
                                device.setLastUserSync(ZonedDateTime.ofInstant(ts, ZoneOffset.UTC));
                                deviceService.createDevice(device); // usa un update real
                            }
                        } catch (Exception ex) {
                            System.err.println("Error getuserlist: " + ex.getMessage());
                        }
                    }, 500);
                }

            } else {
                // ✅ El dispositivo NO EXISTE → igual respondemos SOLO UNA VEZ
                String cloudTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                session.sendMessage(new TextMessage(
                        String.format("{\"ret\":\"reg\",\"result\":true,\"cloudtime\":\"%s\",\"nosenduser\":true}", cloudTime)
                ));
                return;
            }

            // ✅ GETNEWLOG SIEMPRE DESPUÉS
            deviceCommandScheduler.schedule(() -> {
                getNewLogCommandSender.sendGetNewLogCommand(session, true);
            }, 1000);

        } catch (Exception e) {
            try {
                session.sendMessage(new TextMessage("{\"ret\":\"reg\",\"result\":false,\"reason\":\"did not reg\"}"));
                System.out.println("Fallo critico de registro");
            } catch (Exception ignored) {}
        }
    }
}
