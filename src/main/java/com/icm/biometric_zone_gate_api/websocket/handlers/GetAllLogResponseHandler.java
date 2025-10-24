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

    public void handleGetAllLogResponse(JsonNode json, WebSocketSession session) {
        try {
            String sessionId = session.getId();
            if (finishedSessions.getOrDefault(sessionId, false)) {
                System.out.println("Ignorando GETALLLOG: ya finalizado.");
                return;
            }

            boolean result = json.path("result").asBoolean(false);
            String ret = json.path("ret").asText("");
            if (!result || !"getalllog".equalsIgnoreCase(ret)) {
                return;
            }

            int count = json.path("count").asInt(0);
            if (count == 0) {
                finishedSessions.put(sessionId, true);
                System.out.println("No hay más registros (GETALLLOG).");
                return;
            }

            String sn = (String) session.getAttributes().get("sn");
            if (sn == null) return;

            Optional<DeviceModel> optDevice = deviceService.getDeviceBySn(sn);
            if (optDevice.isEmpty()) return;
            DeviceModel device = optDevice.get();

            ArrayNode records = (ArrayNode) json.get("record");
            for (JsonNode record : records) {
                int enrollId = record.path("enrollid").asInt();
                if (enrollId == 0) continue; // Evento del sistema → ignorar

                String timeStr = record.path("time").asText();
                ZonedDateTime logTime = LocalDateTime.parse(timeStr, FORMATTER).atZone(ZoneId.systemDefault());

                Optional<UserModel> optUser = userService.getUserById((long) enrollId);
                if (optUser.isEmpty()) continue;
                UserModel user = optUser.get();

                // 🔹 Duplicado EXACTO → IGNORAR
                if (accessLogsService.findLogByUserDeviceAndTime(user.getId(), device.getId(), logTime).isPresent()) {
                    System.out.println("Log duplicado → ignorado");
                    continue;
                }

                Optional<AccessLogsModel> openLogOpt = accessLogsService.getOpenLogForUserDevice(user, device);

                if (openLogOpt.isEmpty()) {
                    // 🔹 Posible entrada → verificar si recién hubo salida en mismo segundo
                    Optional<AccessLogsModel> lastClosed =
                            accessLogsService.findLastClosedLogByUserDevice(user.getId(), device.getId(), logTime);

                    if (lastClosed.isPresent()) {
                        System.out.println("Entrada ignorada (rebote tras salida en mismo segundo)");
                        continue;
                    }

                    // ✅ Crear nueva ENTRADA
                    AccessLogsModel entry = new AccessLogsModel();
                    entry.setUser(user);
                    entry.setDevice(device);
                    entry.setCompany(device.getCompany());
                    entry.setEventType(eventTypeService.getEventTypeByCode(record.path("event").asInt()).orElse(null));
                    entry.setEntryTime(logTime);
                    entry.setAction(AccessType.ENTRY);
                    entry.setSuccess(true);

                    accessLogsService.createLog(entry);
                    System.out.println("➕ Entrada registrada para usuario " + user.getUsername());

                } else {
                    // Hay un log abierto → posible salida
                    AccessLogsModel entry = openLogOpt.get();

                    long diffSeconds = Duration.between(entry.getEntryTime(), logTime).getSeconds();

                    // 🔹 Salida en el mismo segundo que entrada → rebote
                    if (diffSeconds == 0) {
                        System.out.println("Salida ignorada (rebote: misma hora que entrada)");
                        continue;
                    }

                    // ✅ Cerrar salida
                    entry.setExitTime(logTime);
                    entry.setDurationSeconds(diffSeconds);
                    entry.setAction(AccessType.EXIT);

                    accessLogsService.createLog(entry);
                    System.out.println("✅ Salida registrada para usuario " + user.getUsername());
                }
            }

            // Pedir siguiente paquete
            getAllLogCommandSender.sendGetAllLogCommand(session, false);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
