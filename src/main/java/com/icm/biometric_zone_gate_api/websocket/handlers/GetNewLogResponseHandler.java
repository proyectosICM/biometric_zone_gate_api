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

    public void handleGetNewLogResponse(JsonNode json, WebSocketSession session) {
        try {
            String sessionId = session.getId();

            if (finishedSessions.getOrDefault(sessionId, false)) {
                System.out.println("🛑 Ignorando respuesta de GETNEWLOG: ya finalizado para esta sesión.");
                return;
            }

            boolean result = json.path("result").asBoolean(false);
            String ret = json.path("ret").asText("");

            if (!"getnewlog".equalsIgnoreCase(ret)) {
                System.out.println("⚠️ Respuesta ignorada: no corresponde a 'getnewlog'.");
                return;
            }

            if (!result) {
                int reason = json.path("reason").asInt(-1);
                System.out.printf("⚠️ GETNEWLOG falló. reason=%d%n", reason);
                return;
            }

            int count = json.path("count").asInt(0);
            System.out.printf("✅ GETNEWLOG respuesta: count=%d%n", count);

            String sn = (String) session.getAttributes().get("sn");
            if (sn == null) {
                System.err.println("❌ No se encontró SN en la sesión " + sessionId);
                return;
            }

            Optional<DeviceModel> optDevice = deviceService.getDeviceBySn(sn);
            if (optDevice.isEmpty()) {
                System.err.println("❌ Dispositivo no encontrado con SN=" + sn);
                return;
            }

            DeviceModel device = optDevice.get();

            if (count > 0 && json.has("record") && json.get("record").isArray()) {
                ArrayNode records = (ArrayNode) json.get("record");
                System.out.println("📋 Registros nuevos recibidos (" + records.size() + "):");

                for (JsonNode record : records) {
                    int enrollId = record.path("enrollid").asInt();
                    String timeStr = record.path("time").asText();
                    int inout = record.path("inout").asInt();
                    int eventCode = record.path("event").asInt();

                    System.out.printf(" - ID:%d | Time:%s | InOut:%d | Event:%d%n",
                            enrollId, timeStr, inout, eventCode);

                    if (enrollId == 0 || timeStr.isEmpty()) {
                        System.out.println("ℹ️ Log del sistema (sin usuario), ignorado.");
                        continue;
                    }

                    ZonedDateTime logTime = LocalDateTime.parse(timeStr, FORMATTER)
                            .atZone(ZoneId.systemDefault());

                    Optional<UserModel> optUser = userService.getUserById((long) enrollId);
                    if (optUser.isEmpty()) {
                        System.err.println("⚠️ Usuario no encontrado para enrollId=" + enrollId);
                        continue;
                    }
                    UserModel user = optUser.get();

                    Optional<EventTypeModel> optEventType = eventTypeService.getEventTypeByCode(eventCode);
                    EventTypeModel eventType = optEventType.orElse(null);

                    // 🔎 Evitar duplicados
                    Optional<AccessLogsModel> duplicate =
                            accessLogsService.findLogByUserDeviceAndTime(user.getId(), device.getId(), logTime);
                    if (duplicate.isPresent()) {
                        System.out.println("⌛ Log duplicado (mismo segundo) → IGNORADO");
                        continue;
                    }

                    if (inout == 0) { // 🔵 ENTRADA
                        // Evitar rebote justo después de salida
                        Optional<AccessLogsModel> lastClosed =
                                accessLogsService.findLastClosedLogByUserDevice(user.getId(), device.getId(), logTime);
                        if (lastClosed.isPresent()) {
                            System.out.println("Entrada ignorada (rebote después de salida en el mismo segundo)");
                            continue;
                        }

                        // Si no hay log abierto → crear nuevo
                        Optional<AccessLogsModel> openLog = accessLogsService.getOpenLogForUserDevice(user, device);
                        if (openLog.isEmpty()) {
                            AccessLogsModel log = new AccessLogsModel();
                            log.setEntryTime(logTime);
                            log.setUser(user);
                            log.setDevice(device);
                            log.setCompany(device.getCompany());
                            log.setEventType(eventType);
                            log.setAction(AccessType.ENTRY);
                            log.setSuccess(true);
                            accessLogsService.createLog(log);
                            System.out.println("🟩 Log de ENTRADA registrado para usuario " + user.getUsername());
                        } else {
                            System.out.println("Entrada ignorada (ya hay log abierto sin salida)");
                        }

                    } else { // 🔴 SALIDA
                        Optional<AccessLogsModel> openLog = accessLogsService.getOpenLogForUserDevice(user, device);
                        if (openLog.isPresent()) {
                            AccessLogsModel log = openLog.get();

                            long diffSeconds = Duration.between(log.getEntryTime(), logTime).getSeconds();
                            if (diffSeconds == 0) {
                                System.out.println("Salida duplicada en el mismo segundo → IGNORADA");
                                continue;
                            }
                            if (diffSeconds < 0) {
                                System.out.println("Salida antes de la entrada → IGNORADA");
                                continue;
                            }

                            Optional<AccessLogsModel> lastLogSameTime =
                                    accessLogsService.findLogByUserDeviceAndTime(user.getId(), device.getId(), logTime);
                            if (lastLogSameTime.isPresent()) {
                                System.out.println("Salida duplicada existente en DB → IGNORADA");
                                continue;
                            }

                            log.setExitTime(logTime);
                            log.setDurationSeconds(diffSeconds);
                            log.setAction(AccessType.EXIT);
                            accessLogsService.createLog(log);
                            System.out.println("🟥 Log de SALIDA registrado para usuario " + user.getUsername());
                        } else {
                            System.out.println("⚠️ No se encontró log de entrada abierto para usuario " + user.getUsername());
                        }
                    }
                }

                // 🔁 Solicitar siguiente paquete
                System.out.println("⏳ Solicitando siguiente paquete de logs (GETNEWLOG)...");
                getNewLogCommandSender.sendGetNewLogCommand(session, false);

            } else {
                System.out.println("📭 No hay más registros nuevos. Fin del GETNEWLOG.");
                finishedSessions.put(sessionId, true);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al procesar GETNEWLOG: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
