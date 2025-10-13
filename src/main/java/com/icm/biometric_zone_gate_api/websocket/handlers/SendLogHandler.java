package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.enums.AccessType;
import com.icm.biometric_zone_gate_api.models.AccessLogsModel;
import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.models.UserModel;
import com.icm.biometric_zone_gate_api.services.AccessLogsService;
import com.icm.biometric_zone_gate_api.services.DeviceService;
import com.icm.biometric_zone_gate_api.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SendLogHandler {

    private final DeviceService deviceService;
    private final UserService userService;
    private final AccessLogsService accessLogsService;

    public void handleSendLog(JsonNode json, WebSocketSession session) {
        try {
            int count = json.path("count").asInt(0);
            JsonNode records = json.path("record");

            if (count <= 0 || !records.isArray() || records.size() != count) {
                System.err.println("Invalid logs: count does not match records");
                session.sendMessage(new TextMessage("{\"ret\":\"sendlog\",\"result\":false,\"reason\":1}"));
                return;
            }

            System.out.println("Received logs from device: " + session.getId());
            for (JsonNode record : records) {
                int enrollId = record.path("enrollid").asInt(0);
                String time = record.path("time").asText("");
                int mode = record.path("mode").asInt(0);
                int inout = record.path("inout").asInt(0);
                int event = record.path("event").asInt(0);

                System.out.printf("Log: enrollid=%d, time=%s, mode=%d, inout=%d, event=%d%n",
                        enrollId, time, mode, inout, event);

                if (enrollId != 0) {
                    UserModel user = userService.getUserById(enrollId).orElse(null);
                    if (user == null) continue;

                    DeviceModel device = deviceService.getDeviceBySn(sn).orElse(null);
                    if (device == null) continue;

                    if (inout == 0) { // Entrada
                        AccessLogsModel log = new AccessLogsModel();
                        log.setEntryTime(logTime);
                        log.setUser(user);
                        log.setDevice(device);
                        log.setCompany(company);
                        log.setAction(AccessType.IN);
                        log.setSuccess(true);
                        log.setCorrectEpp(false);
                        log.setEventType(eventTypeService.getDefaultEventType()); // ejemplo
                        accessLogsService.createLog(log);
                    }  else { // Salida
                        Optional<AccessLogsModel> openLog = accessLogsService.getOpenLogForUserDevice(user, device);
                        if (openLog.isPresent()) {
                            AccessLogsModel log = openLog.get();
                            log.setExitTime(logTime);
                            log.setDurationSeconds(Duration.between(log.getEntryTime().toInstant(), logTime.toInstant()).getSeconds());
                            log.setAction(AccessType.EXIT);
                            accessLogsService.createLog(log); // update
                        }
                }
            }

            // Server time
            String cloudTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // Simulate access = 1 (door open)
            String response = String.format("{\"ret\":\"sendlog\",\"result\":true,\"cloudtime\":\"%s\",\"access\":1}", cloudTime);
            session.sendMessage(new TextMessage(response));

        } catch (Exception e) {
            e.printStackTrace();
            try {
                session.sendMessage(new TextMessage("{\"ret\":\"sendlog\",\"result\":false,\"reason\":1}"));
            } catch (Exception ignored) {}
        }
    }
}
