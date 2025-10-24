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

    public void handleGetAllLogResponse(JsonNode json, WebSocketSession session) {
        try {
            String sessionId = session.getId();

            if (finishedSessions.getOrDefault(sessionId, false)) {
                System.out.println("Ignorando respuesta de GETALLLOG: ya finalizado para esta sesión.");
                return;
            }

            boolean result = json.path("result").asBoolean(false);
            String ret = json.path("ret").asText("");

            if (!"getalllog".equalsIgnoreCase(ret)) {
                System.out.println("Respuesta ignorada: no corresponde a 'getalllog'.");
                return;
            }

            if (!result) {
                int reason = json.path("reason").asInt(-1);
                System.out.printf("GETALLLOG falló. reason=%d%n", reason);
                return;
            }

            int count = json.path("count").asInt(0);
            int from = json.path("from").asInt(0);
            int to = json.path("to").asInt(0);

            System.out.printf("GETALLLOG respuesta: count=%d, from=%d, to=%d%n", count, from, to);

            String sn = (String) session.getAttributes().get("sn");
            if (sn == null) {
                System.err.println("No se encontró SN en la sesión " + sessionId);
                return;
            }

            Optional<DeviceModel> optDevice = deviceService.getDeviceBySn(sn);
            if (optDevice.isEmpty()) {
                System.err.println("Dispositivo no encontrado con SN=" + sn);
                return;
            }
            DeviceModel device = optDevice.get();

            if (count > 0 && json.has("record") && json.get("record").isArray()) {
                ArrayNode records = (ArrayNode) json.get("record");
                System.out.println("📋 Registros recibidos (" + records.size() + "):");

                for (JsonNode record : records) {
                    int enrollId = record.path("enrollid").asInt();
                    String time = record.path("time").asText();
                    int mode = record.path("mode").asInt();
                    int inout = record.path("inout").asInt();
                    int event = record.path("event").asInt();

                    System.out.printf(" - ID:%d | Time:%s | Mode:%d | InOut:%d | Event:%d%n",
                            enrollId, time, mode, inout, event);

                    ZonedDateTime logTime = LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            .atZone(ZoneId.systemDefault());

                    Optional<EventTypeModel> optEventType = eventTypeService.getEventTypeByCode(event);
                    EventTypeModel eventType = optEventType.orElse(null);

                    if (enrollId == 0) {
                        System.out.println("Log del sistema ignorado (sin usuario).");
                        continue;
                    }

                    Optional<UserModel> optUser = userService.getUserById((long) enrollId);
                    if (optUser.isEmpty()) {
                        System.err.println("Usuario no encontrado para enrollId=" + enrollId);
                        continue;
                    }

                    UserModel user = optUser.get();

                    if (inout == 0) {
                        Optional<AccessLogsModel> duplicateExitCheck =
                                accessLogsService.findLogByUserDeviceAndTime(user.getId(), device.getId(), logTime);

                        if (duplicateExitCheck.isPresent()) {
                            System.out.println("⚠ Entrada duplicada (ya hubo log en este segundo - rebote) → IGNORADA");
                            continue;
                        }

                        // Check si se acaba de cerrar una salida en el mismo instante
                        Optional<AccessLogsModel> lastClosed =
                                accessLogsService.findLastClosedLogByUserDevice(user.getId(), device.getId(), logTime);

                        if (lastClosed.isPresent()) {
                            System.out.println("Entrada ignorada porque hubo una salida en el mismo instante (rebote) → IGNORADA");
                            continue;
                        }

                        AccessLogsModel log = new AccessLogsModel();
                        log.setEntryTime(logTime);
                        log.setUser(user);
                        log.setDevice(device);
                        log.setCompany(device.getCompany());
                        log.setEventType(eventType);
                        log.setAction(AccessType.ENTRY);
                        log.setSuccess(true);

                        accessLogsService.createLog(log);
                        System.out.println("Log de ENTRADA registrado para usuario " + user.getUsername());
                    } else {
                        Optional<AccessLogsModel> openLog = accessLogsService.getOpenLogForUserDevice(user, device);
                        if (openLog.isPresent()) {
                            AccessLogsModel log = openLog.get();

                            long diffSeconds = Duration.between(log.getEntryTime(), logTime).getSeconds();

                            // 1 mismo instante = rebote
                            if (diffSeconds == 0) {
                                System.out.println("Salida duplicada en el mismo segundo → IGNORADA");
                                continue;
                            }

                            // 2 Duración negativa o extraña
                            if (diffSeconds < 0) {
                                System.out.println("Evento de salida antes que la entrada → IGNORADO");
                                continue;
                            }

                            // 3 (opcional) Si ya existe una SALIDA en mismo segundo (seguridad extra)
                            Optional<AccessLogsModel> lastLogSameTime =
                                    accessLogsService.findLogByUserDeviceAndTime(user.getId(), device.getId(), logTime);
                            if (lastLogSameTime.isPresent()) {
                                System.out.println("Salida duplicada existente en DB → IGNORADA");
                                continue;
                            }

                            log.setExitTime(logTime);
                            long duration = Duration.between(log.getEntryTime(), logTime).getSeconds();
                            log.setDurationSeconds(duration);
                            log.setAction(AccessType.EXIT);
                            accessLogsService.createLog(log);
                            System.out.println("🟥 Log de SALIDA registrado para usuario " + user.getUsername());
                        } else {
                            System.out.println("⚠️ No se encontró log de entrada abierto para usuario " + user.getUsername());
                        }
                    }
                }

                // pide siguiente paquete
                System.out.println("⏳ Solicitando siguiente paquete de logs (GETALLLOG)...");
                getAllLogCommandSender.sendGetAllLogCommand(session, false);

            } else {
                System.out.println("📭 No hay más registros. Fin del GETALLLOG.");
                finishedSessions.put(sessionId, true);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al procesar GETALLLOG: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
