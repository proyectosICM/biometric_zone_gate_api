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

    public void handleGetNewLogResponse(JsonNode json, WebSocketSession session) {
        try {
            String sessionId = session.getId();

            if (finishedSessions.getOrDefault(sessionId, false)) {
                System.out.println("🛑 Ignorando respuesta de GETNEWLOG: ya se marcó como finalizado.");
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
            int from = json.path("from").asInt(0);
            int to = json.path("to").asInt(0);

            System.out.printf("✅ GETNEWLOG respuesta: count=%d, from=%d, to=%d%n", count, from, to);

            // 🧠 Obtener SN del dispositivo asociado a la sesión
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
                System.out.println("📋 Registros recibidos (" + records.size() + "):");

                for (JsonNode record : records) {
                    int enrollId = record.path("enrollid").asInt();
                    String time = record.path("time").asText();
                    int mode = record.path("mode").asInt();
                    int inout = record.path("inout").asInt();
                    int event = record.path("event").asInt();

                    System.out.printf(" - ID:%d | Time:%s | Mode:%d | InOut:%d | Event:%d%n",
                            enrollId, time, mode, inout, event);

                    // 📅 Parsear hora
                    ZonedDateTime logTime = LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            .atZone(ZoneId.systemDefault());

                    // 🔍 Buscar tipo de evento
                    Optional<EventTypeModel> optEventType = eventTypeService.getEventTypeByCode(event);
                    EventTypeModel eventType = optEventType.orElse(null);
                    if (eventType == null) {
                        System.err.println("⚠️ Tipo de evento no encontrado para code=" + event);
                    }

                    // Logs del sistema
                    if (enrollId == 0) {
                        System.out.println("ℹ️ Log del sistema ignorado (sin usuario).");
                        continue;
                    }

                    // 🔍 Buscar usuario
                    Optional<UserModel> optUser = userService.getUserById((long) enrollId);
                    if (optUser.isEmpty()) {
                        System.err.println("⚠️ Usuario no encontrado para enrollId=" + enrollId);
                        continue;
                    }

                    UserModel user = optUser.get();

                    // 🧾 Crear / actualizar log
                    if (inout == 0) { // Entrada
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

                    } else { // Salida
                        Optional<AccessLogsModel> openLog = accessLogsService.getOpenLogForUserDevice(user, device);
                        if (openLog.isPresent()) {
                            AccessLogsModel log = openLog.get();
                            log.setExitTime(logTime);
                            long duration = Duration.between(log.getEntryTime(), logTime).getSeconds();
                            log.setDurationSeconds(duration);
                            log.setAction(AccessType.EXIT);
                            accessLogsService.createLog(log); // o update si tu servicio lo maneja así
                            System.out.println("🟥 Log de SALIDA registrado para usuario " + user.getUsername());
                        } else {
                            System.out.println("⚠️ No se encontró log de entrada abierto para usuario " + user.getUsername());
                        }
                    }
                }

                // 🔁 Solicitar siguiente paquete
                System.out.println("⏳ Solicitando siguiente paquete de logs...");
                getNewLogCommandSender.sendGetNewLogCommand(session, false);

            } else {
                System.out.println("📭 No hay más registros nuevos. Fin del ciclo GETNEWLOG.");
                finishedSessions.put(sessionId, true);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al procesar respuesta de GETNEWLOG: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
