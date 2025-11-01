package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.icm.biometric_zone_gate_api.enums.AccessType;
import com.icm.biometric_zone_gate_api.enums.CredentialType;
import com.icm.biometric_zone_gate_api.models.AccessLogsModel;
import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.models.EventTypeModel;
import com.icm.biometric_zone_gate_api.models.UserModel;
import com.icm.biometric_zone_gate_api.services.AccessLogsService;
import com.icm.biometric_zone_gate_api.services.DeviceService;
import com.icm.biometric_zone_gate_api.services.EventTypeService;
import com.icm.biometric_zone_gate_api.services.UserService;
import com.icm.biometric_zone_gate_api.websocket.commands.GetAllLogCommandSender;
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
public class GetAllLogResponseHandler {

    private final GetAllLogCommandSender getAllLogCommandSender;
    private final DeviceService deviceService;
    private final UserService userService;
    private final AccessLogsService accessLogsService;
    private final EventTypeService eventTypeService;

    private final ConcurrentHashMap<String, Boolean> finishedSessions = new ConcurrentHashMap<>();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MIN_SECONDS_BETWEEN_EVENTS = 5;

    public void handleGetAllLogResponse(JsonNode json, WebSocketSession session) {
        try {
            String sessionId = session.getId();
            if (finishedSessions.getOrDefault(sessionId, false)) return;

            boolean result = json.path("result").asBoolean(false);
            String ret = json.path("ret").asText("");
            if (!result || !"getalllog".equalsIgnoreCase(ret)) return;

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

                int mode = record.has("mode") ? record.path("mode").asInt()
                        : record.path("attmode").asInt(record.path("verify").asInt(-1));
                CredentialType authMode = mapLogModeToCredentialType(mode);

                String timeStr = record.path("time").asText();
                ZonedDateTime logTime = LocalDateTime.parse(timeStr, FORMATTER).atZone(ZoneId.systemDefault());

                Optional<UserModel> optUser = userService.getUserById((long) enrollId);
                if (optUser.isEmpty()) continue;
                UserModel user = optUser.get();

                // 🔹 1) Duplicado EXACTO → IGNORAR
                if (accessLogsService.findLogByUserDeviceAndTime(user.getId(), device.getId(), logTime).isPresent()) {
                    continue;
                }

                Optional<AccessLogsModel> openLogOpt = accessLogsService.getOpenLogForUserDevice(user, device);

                if (openLogOpt.isEmpty()) {

                    // 🔹 2) Rebote tras salida en mismo segundo
                    if (accessLogsService.findLastClosedLogByUserDevice(user.getId(), device.getId(), logTime).isPresent()) {
                        continue;
                    }

                    // ✅ Crear entrada
                    AccessLogsModel entry = new AccessLogsModel();
                    entry.setUser(user);
                    entry.setDevice(device);
                    entry.setCompany(device.getCompany());
                    entry.setEventType(eventTypeService.getEventTypeByCode(record.path("event").asInt()).orElse(null));
                    entry.setEntryTime(logTime);
                    entry.setAction(AccessType.ENTRY);
                    entry.setSuccess(true);
                    entry.setEntryAuthMode(authMode);
                    accessLogsService.createLog(entry);

                } else {
                    AccessLogsModel entry = openLogOpt.get();
                    long diffSeconds = Duration.between(entry.getEntryTime(), logTime).getSeconds();

                    // 3️⃣ evitar negativos
                    if (diffSeconds < 0) continue;

                    // 4️⃣ evitar rebotes si < 5 segundos
                    if (diffSeconds < MIN_SECONDS_BETWEEN_EVENTS) continue;

                    // ✅ SALIDA válida
                    entry.setExitTime(logTime);
                    entry.setDurationSeconds(diffSeconds);
                    entry.setAction(AccessType.EXIT);
                    entry.setExitAuthMode(authMode != CredentialType.UNKNOWN ? authMode : entry.getEntryAuthMode());
                    accessLogsService.createLog(entry);
                }
            }

            getAllLogCommandSender.sendGetAllLogCommand(session, false);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private CredentialType mapLogModeToCredentialType(int mode) {
        switch (mode) {
            case 0:  return CredentialType.FINGERPRINT; // huella
            case 1:  return CredentialType.CARD;        // tarjeta
            case 2:  return CredentialType.PASSWORD;    // contraseña
            case 8:  return CredentialType.PHOTO;       // rostro
            default: return CredentialType.UNKNOWN;
        }
    }
}
