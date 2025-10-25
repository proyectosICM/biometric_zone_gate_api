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

// ... (imports y demás igual)

@Component
@RequiredArgsConstructor
public class SendLogHandler {

    private final DeviceService deviceService;
    private final UserService userService;
    private final AccessLogsService accessLogsService;
    private final EventTypeService eventTypeService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MIN_SECONDS_BETWEEN_EVENTS = 5;

    public void handleSendLog(JsonNode json, WebSocketSession session) {
        try {
            int count = json.path("count").asInt(0);
            JsonNode records = json.path("record");

            if (count <= 0 || !records.isArray() || records.size() != count) {
                session.sendMessage(new TextMessage("{\"ret\":\"sendlog\",\"result\":false,\"reason\":1}"));
                return;
            }

            // SN
            String sn = (String) session.getAttributes().get("sn");
            Optional<DeviceModel> optDevice = deviceService.getDeviceBySn(sn);
            if (optDevice.isEmpty()) {
                session.sendMessage(new TextMessage("{\"ret\":\"sendlog\",\"result\":false,\"reason\":1}"));
                return;
            }
            DeviceModel device = optDevice.get();

            for (JsonNode record : records) {
                int enrollId = record.path("enrollid").asInt();
                if (enrollId == 0) continue;

                String timeStr = record.path("time").asText();
                ZonedDateTime logTime = LocalDateTime.parse(timeStr, FORMATTER).atZone(ZoneId.systemDefault());

                Optional<UserModel> optUser = userService.getUserById((long) enrollId);
                if (optUser.isEmpty()) continue;
                UserModel user = optUser.get();

                // 1️⃣ Duplicado exacto
                if (accessLogsService.findLogByUserDeviceAndTime(user.getId(), device.getId(), logTime).isPresent()) {
                    continue;
                }

                Optional<AccessLogsModel> openLogOpt = accessLogsService.getOpenLogForUserDevice(user, device);

                if (openLogOpt.isEmpty()) {

                    // 2️⃣ Recién se cerró una salida en el mismo instante → rebote
                    if (accessLogsService.findLastClosedLogByUserDevice(user.getId(), device.getId(), logTime).isPresent()) {
                        continue;
                    }

                    // Crear entrada
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

                    // Cerrar anterior si tiene sentido
                    AccessLogsModel entry = openLogOpt.get();
                    long diffSeconds = Duration.between(entry.getEntryTime(), logTime).getSeconds();

                    // 3️⃣ si el cierre es ANTES que la apertura → ERROR → ignorar
                    if (diffSeconds < 0) {
                        continue;
                    }

                    // 4️⃣ rebote dentro de la ventana
                    if (diffSeconds < MIN_SECONDS_BETWEEN_EVENTS) {
                        continue;
                    }

                    // ✅ cerrar log
                    entry.setExitTime(logTime);
                    entry.setDurationSeconds(diffSeconds);
                    entry.setAction(AccessType.EXIT);
                    accessLogsService.createLog(entry);
                }
            }

            session.sendMessage(new TextMessage(
                    String.format("{\"ret\":\"sendlog\",\"result\":true,\"cloudtime\":\"%s\",\"access\":1}",
                            LocalDateTime.now().format(FORMATTER))
            ));

        } catch (Exception e) {
            try {
                session.sendMessage(new TextMessage("{\"ret\":\"sendlog\",\"result\":false,\"reason\":1}"));
            } catch (Exception ignored) {}
        }
    }
}
