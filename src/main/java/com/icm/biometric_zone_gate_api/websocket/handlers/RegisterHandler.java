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
                session.sendMessage(new TextMessage("{\"ret\":\"reg\",\"result\":false,\"reason\":\"did not reg\"}"));
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

                // ✅ RESPONDEMOS SOLO UNA VEZ
                String cloudTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                session.sendMessage(new TextMessage(
                        String.format("{\"ret\":\"reg\",\"result\":true,\"cloudtime\":\"%s\",\"nosenduser\":true}", cloudTime)
                ));

                // ✅ LUEGO evaluamos si se sincroniza
                LocalDateTime now = LocalDateTime.now();
                int minute = now.getMinute();
                boolean inSyncWindow = (minute % 10 <= 2);

                if (inSyncWindow) {
                    ZonedDateTime lastSync = device.getLastUserSync();
                    boolean mustSync = (lastSync == null) ||
                            Duration.between(lastSync, now.atZone(ZoneId.systemDefault())).toMinutes() >= MINUTES_BETWEEN_SYNC;

                    if (mustSync) {
                        deviceCommandScheduler.schedule(() ->
                                getUserListCommandSender.sendGetUserListCommand(session, true), 500
                        );
                        device.setLastUserSync(ZonedDateTime.now());
                        deviceService.createDevice(device);
                    }
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
            } catch (Exception ignored) {}
        }
    }
}
