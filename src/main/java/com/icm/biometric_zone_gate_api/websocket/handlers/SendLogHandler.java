package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.enums.AccessType;
import com.icm.biometric_zone_gate_api.models.AccessLogsModel;
import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.models.EventTypeModel;
import com.icm.biometric_zone_gate_api.models.UserModel;
import com.icm.biometric_zone_gate_api.services.AccessLogsService;
import com.icm.biometric_zone_gate_api.services.DeviceService;
import com.icm.biometric_zone_gate_api.services.EventTypeService;
import com.icm.biometric_zone_gate_api.services.UserService;
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
public class SendLogHandler {

    private final DeviceService deviceService;
    private final UserService userService;
    private final AccessLogsService accessLogsService;
    private final EventTypeService eventTypeService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void handleSendLog(JsonNode json, WebSocketSession session) {
        try {
            int count = json.path("count").asInt(0);
            JsonNode records = json.path("record");

            if (count <= 0 || !records.isArray() || records.size() != count) {
                session.sendMessage(new TextMessage("{\"ret\":\"sendlog\",\"result\":false,\"reason\":1}"));
                return;
            }

            String sn = (String) session.getAttributes().get("sn");
            Optional<DeviceModel> optDevice = deviceService.getDeviceBySn(sn);
            if (optDevice.isEmpty()) {
                session.sendMessage(new TextMessage("{\"ret\":\"sendlog\",\"result\":false,\"reason\":1}"));
                return;
            }
            DeviceModel device = optDevice.get();

            for (JsonNode record : records) {
                int enrollId = record.path("enrollid").asInt();
                if (enrollId == 0) continue; // system event

                String timeStr = record.path("time").asText();
                ZonedDateTime logTime = LocalDateTime.parse(timeStr, FORMATTER).atZone(ZoneId.systemDefault());

                Optional<UserModel> optUser = userService.getUserById((long) enrollId);
                if (optUser.isEmpty()) continue;
                UserModel user = optUser.get();

                // ✅ 1. Evitar duplicado exacto
                if (accessLogsService.findLogByUserDeviceAndTime(user.getId(), device.getId(), logTime).isPresent()) {
                    continue;
                }

                Optional<AccessLogsModel> openLogOpt = accessLogsService.getOpenLogForUserDevice(user, device);

                if (openLogOpt.isEmpty()) {
                    // ✅ 2. Check rebote: salida recién cerrada en mismo segundo => no crear
                    if (accessLogsService.findLastClosedLogByUserDevice(user.getId(), device.getId(), logTime).isPresent()) {
                        continue;
                    }

                    // ➕ CREAR NUEVA ENTRADA
                    AccessLogsModel entry = new AccessLogsModel();
                    entry.setUser(user);
                    entry.setDevice(device);
                    entry.setCompany(device.getCompany());
                    entry.setEventType(eventTypeService.getEventTypeByCode(record.path("event").asInt()).orElse(null));
                    entry.setEntryTime(logTime);
                    entry.setAction(AccessType.ENTRY);
                    entry.setSuccess(true);
                    accessLogsService.createLog(entry);

                } else {
                    // Cerrar la anterior
                    AccessLogsModel entry = openLogOpt.get();
                    long diff = Duration.between(entry.getEntryTime(), logTime).getSeconds();

                    if (diff == 0) continue; // Rebote

                    entry.setExitTime(logTime);
                    entry.setDurationSeconds(diff);
                    entry.setAction(AccessType.EXIT);
                    accessLogsService.createLog(entry);
                }
            }

            session.sendMessage(new TextMessage(
                    String.format("{\"ret\":\"sendlog\",\"result\":true,\"cloudtime\":\"%s\",\"access\":1}",
                            LocalDateTime.now().format(FORMATTER))
            ));

        } catch (Exception e) {
            try { session.sendMessage(new TextMessage("{\"ret\":\"sendlog\",\"result\":false,\"reason\":1}")); }
            catch (Exception ignored) {}
        }
    }
}
