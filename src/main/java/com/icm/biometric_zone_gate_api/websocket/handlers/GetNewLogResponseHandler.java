package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.icm.biometric_zone_gate_api.enums.AccessType;
import com.icm.biometric_zone_gate_api.models.AccessLogsModel;
import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.models.EventTypeModel;
import com.icm.biometric_zone_gate_api.models.UserModel;
import com.icm.biometric_zone_gate_api.services.AccessLogsService;
import com.icm.biometric_zone_gate_api.services.DeviceService;
import com.icm.biometric_zone_gate_api.services.EventTypeService;
import com.icm.biometric_zone_gate_api.services.UserService;
import com.icm.biometric_zone_gate_api.websocket.commands.GetNewLogCommandSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class GetNewLogResponseHandler {

    private final GetNewLogCommandSender getNewLogCommandSender;
    private final DeviceService deviceService;
    private final UserService userService;
    private final AccessLogsService accessLogsService;
    private final EventTypeService eventTypeService;

    private final ConcurrentHashMap<String, Boolean> finishedSessions = new ConcurrentHashMap<>();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MIN_SECONDS_BETWEEN_EVENTS = 5;

    public void handleGetNewLogResponse(JsonNode json, WebSocketSession session) {
        try {
            String sessionId = session.getId();
            if (finishedSessions.getOrDefault(sessionId, false)) {
                return;
            }

            boolean result = json.path("result").asBoolean(false);
            String ret = json.path("ret").asText("");
            if (!result || !"getnewlog".equalsIgnoreCase(ret)) {
                return;
            }

            int count = json.path("count").asInt(0);
            if (count == 0) {
                finishedSessions.put(sessionId, true);
                return;
            }

            String sn = (String) session.getAttributes().get("sn");
            Optional<DeviceModel> optDevice = deviceService.getDeviceBySn(sn);
            if (optDevice.isEmpty()) return;
            DeviceModel device = optDevice.get();

            ArrayNode records = (ArrayNode) json.get("record");

            for (JsonNode record : records) {
                int enrollId = record.path("enrollid").asInt();
                if (enrollId == 0) continue;

                String timeStr = record.path("time").asText();
                ZonedDateTime logTime = LocalDateTime.parse(timeStr, FORMATTER).atZone(ZoneId.systemDefault());

                Optional<UserModel> optUser = userService.getUserById((long) enrollId);
                if (optUser.isEmpty()) continue;
                UserModel user = optUser.get();

                // ✅ Evitar duplicado exacto
                if (accessLogsService.findLogByUserDeviceAndTime(user.getId(), device.getId(), logTime).isPresent()) {
                    continue;
                }

                Optional<AccessLogsModel> openLogOpt = accessLogsService.getOpenLogForUserDevice(user, device);

                if (openLogOpt.isEmpty()) {

                    // ✅ Evitar rebote justo después de una salida
                    if (accessLogsService.findLastClosedLogByUserDevice(user.getId(), device.getId(), logTime).isPresent()) {
                        continue;
                    }

                    // Crear nueva entrada
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
                    // Cerrar log si corresponde
                    AccessLogsModel existing = openLogOpt.get();
                    long diffSeconds = Duration.between(existing.getEntryTime(), logTime).getSeconds();

                    // ❌ No permitir cierre si es antes de la entrada
                    if (diffSeconds < 0) continue;

                    // ❌ No cerrar dentro de la ventana mínima
                    if (diffSeconds < MIN_SECONDS_BETWEEN_EVENTS) continue;

                    // ✅ Cerrar correctamente
                    existing.setExitTime(logTime);
                    existing.setDurationSeconds(diffSeconds);
                    existing.setAction(AccessType.EXIT);
                    accessLogsService.createLog(existing);
                }
            }

            getNewLogCommandSender.sendGetNewLogCommand(session, false);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
