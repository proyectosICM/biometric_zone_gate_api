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

    public void handleSendLog(JsonNode json, WebSocketSession session) {
        try {
            int count = json.path("count").asInt(0);
            JsonNode records = json.path("record");

            if (count <= 0 || !records.isArray() || records.size() != count) {
                System.err.println("Invalid logs: count does not match records");
                session.sendMessage(new TextMessage("{\"ret\":\"sendlog\",\"result\":false,\"reason\":1}"));
                return;
            }

            // Obtener el SN de la sesión WebSocket
            String sn = (String) session.getAttributes().get("sn");
            if (sn == null) {
                System.err.println("No SN found for session " + session.getId());
                session.sendMessage(new TextMessage("{\"ret\":\"sendlog\",\"result\":false,\"reason\":1}"));
                return;
            }

            Optional<DeviceModel> optDevice = deviceService.getDeviceBySn(sn);
            if (optDevice.isEmpty()) {
                System.err.println("Device not found for SN: " + sn);
                session.sendMessage(new TextMessage("{\"ret\":\"sendlog\",\"result\":false,\"reason\":1}"));
                return;
            }

            DeviceModel device = optDevice.get();

            System.out.println("Received logs from device: " + sn);
            for (JsonNode record : records) {
                int enrollId = record.path("enrollid").asInt(0);
                String time = record.path("time").asText("");
                int mode = record.path("mode").asInt(0);
                int inout = record.path("inout").asInt(0);
                int eventCode = record.path("event").asInt(0);

                System.out.printf("Log: enrollid=%d, time=%s, mode=%d, inout=%d, event=%d%n",
                        enrollId, time, mode, inout, eventCode);

                // Parsear fecha
                ZonedDateTime logTime = LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        .atZone(ZoneId.systemDefault());

                // Buscar tipo de evento
                Optional<EventTypeModel> optEventType = eventTypeService.getEventTypeByCode(eventCode);
                EventTypeModel eventType = optEventType.orElse(null);
                if (eventType == null) {
                    System.err.println("Tipo de evento no encontrado para code=" + eventCode);
                }

                // Si enrollId == 0 → log del sistema (por ejemplo puerta abierta/cerrada)
                if (enrollId == 0) {
                    System.out.println("System event from device " + sn + ": event=" + eventCode);
                    continue;
                }

                // Buscar usuario
                Optional<UserModel> optUser = userService.getUserById((long) enrollId);
                if (optUser.isEmpty()) {
                    System.err.println("User not found for enrollId=" + enrollId);
                    continue;
                }

                UserModel user = optUser.get();

                // Buscar log abierto (sin salida) para este usuario y dispositivo
                Optional<AccessLogsModel> openLogOpt = accessLogsService.getOpenLogForUserDevice(user, device);

                // Crear o actualizar log
                if (inout == 0) { // Entrada
                    // Si hay un log abierto anterior, se cierra automáticamente
                    if (openLogOpt.isPresent()) {
                        AccessLogsModel oldLog = openLogOpt.get();
                        oldLog.setExitTime(logTime);
                        long duration = Duration.between(oldLog.getEntryTime(), logTime).getSeconds();
                        oldLog.setDurationSeconds(duration);
                        oldLog.setAction(AccessType.EXIT);
                        accessLogsService.createLog(oldLog);
                        System.out.printf("⚠️ Se cerró log anterior abierto del usuario %s%n", user.getUsername());
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
                } else { // Salida
                    Optional<AccessLogsModel> openLog = accessLogsService.getOpenLogForUserDevice(user, device);
                    if (openLog.isPresent()) {
                        AccessLogsModel log = openLog.get();
                        log.setExitTime(logTime);
                        long duration = Duration.between(log.getEntryTime(), logTime).getSeconds();
                        log.setDurationSeconds(duration);
                        log.setAction(AccessType.EXIT);
                        accessLogsService.createLog(log); // o update, depende de tu servicio
                    }
                }
            }

            // Server time
            String cloudTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // Responder al dispositivo
            String response = String.format(
                    "{\"ret\":\"sendlog\",\"result\":true,\"cloudtime\":\"%s\",\"access\":1}",
                    cloudTime
            );
            session.sendMessage(new TextMessage(response));

        } catch (Exception e) {
            e.printStackTrace();
            try {
                session.sendMessage(new TextMessage("{\"ret\":\"sendlog\",\"result\":false,\"reason\":1}"));
            } catch (Exception ignored) {
            }
        }
    }
}
