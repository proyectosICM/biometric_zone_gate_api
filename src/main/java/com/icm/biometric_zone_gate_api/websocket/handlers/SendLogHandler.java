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
                session.sendMessage(new TextMessage("{\"ret\":\"sendlog\",\"result\":false,\"reason\":1}"));
                return;
            }

            String sn = (String) session.getAttributes().get("sn");
            if (sn == null) {
                session.sendMessage(new TextMessage("{\"ret\":\"sendlog\",\"result\":false,\"reason\":1}"));
                return;
            }

            Optional<DeviceModel> optDevice = deviceService.getDeviceBySn(sn);
            if (optDevice.isEmpty()) {
                session.sendMessage(new TextMessage("{\"ret\":\"sendlog\",\"result\":false,\"reason\":1}"));
                return;
            }

            DeviceModel device = optDevice.get();

            for (JsonNode record : records) {
                int enrollId = record.path("enrollid").asInt(0);
                String time = record.path("time").asText("");
                int inout = record.path("inout").asInt(0);
                int eventCode = record.path("event").asInt(0);

                if (enrollId == 0) continue;

                ZonedDateTime logTime = LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        .atZone(ZoneId.systemDefault());

                Optional<UserModel> optUser = userService.getUserById((long) enrollId);
                if (optUser.isEmpty()) continue;
                UserModel user = optUser.get();

                Optional<AccessLogsModel> duplicate =
                        accessLogsService.findLogByUserDeviceAndTime(user.getId(), device.getId(), logTime);
                if (duplicate.isPresent()) continue;

                Optional<EventTypeModel> eventType = eventTypeService.getEventTypeByCode(eventCode);
                Optional<AccessLogsModel> openLogOpt = accessLogsService.getOpenLogForUserDevice(user, device);

                if (inout == 0) { // ENTRADA

                    Optional<AccessLogsModel> lastClosed =
                            accessLogsService.findLastClosedLogByUserDevice(user.getId(), device.getId(), logTime);
                    if (lastClosed.isPresent()) continue;

                    if (openLogOpt.isEmpty()) {
                        AccessLogsModel log = new AccessLogsModel();
                        log.setEntryTime(logTime);
                        log.setUser(user);
                        log.setDevice(device);
                        log.setCompany(device.getCompany());
                        log.setEventType(eventType.orElse(null));
                        log.setAction(AccessType.ENTRY);
                        log.setSuccess(true);
                        accessLogsService.createLog(log);
                    }

                } else { // SALIDA
                    if (openLogOpt.isPresent()) {
                        AccessLogsModel log = openLogOpt.get();
                        long diffSec = Duration.between(log.getEntryTime(), logTime).getSeconds();
                        if (diffSec > 0) {
                            log.setExitTime(logTime);
                            log.setDurationSeconds(diffSec);
                            log.setAction(AccessType.EXIT);
                            accessLogsService.createLog(log);
                        }
                    }
                }
            }

            String cloudTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            session.sendMessage(new TextMessage(
                    "{\"ret\":\"sendlog\",\"result\":true,\"cloudtime\":\"" + cloudTime + "\",\"access\":1}"
            ));

        } catch (Exception e) {
            try { session.sendMessage(new TextMessage("{\"ret\":\"sendlog\",\"result\":false,\"reason\":1}")); }
            catch (Exception ignored) {}
        }
    }
}
