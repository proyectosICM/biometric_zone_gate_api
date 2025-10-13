package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.enums.DeviceStatus;
import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.services.DeviceService;
import com.icm.biometric_zone_gate_api.websocket.utils.DeviceValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@RequiredArgsConstructor
public class RegisterHandler {

    private final DeviceService deviceService;

    public void handleRegister(JsonNode json, WebSocketSession session) {
        try {
            String sn = json.path("sn").asText(null);
            if (sn == null || sn.isEmpty()) {
                System.err.println("Invalid registration: missing SN");
                session.sendMessage(new TextMessage("{\"ret\":\"reg\", \"result\":false, \"reason\":1}"));
                return;
            }

            JsonNode devinfo = json.path("devinfo");
            if (!DeviceValidator.validateDevInfo(devinfo)) {
                System.err.println("Invalid registration: incomplete or incorrect devinfo");
                session.sendMessage(new TextMessage("{\"ret\":\"reg\", \"result\":false, \"reason\":1}"));
                return;
            }

            // Extraer todos los campos del devinfo
            String model = devinfo.path("modelname").asText("");
            String firmware = devinfo.path("firmware").asText("");
            int usersize = devinfo.path("usersize").asInt(0);
            int fpsize = devinfo.path("fpsize").asInt(0);
            int cardsize = devinfo.path("cardsize").asInt(0);
            int pwdsize = devinfo.path("pwdsize").asInt(0);
            int logsize = devinfo.path("logsize").asInt(0);
            int useduser = devinfo.path("useduser").asInt(0);
            int usedfp = devinfo.path("usedfp").asInt(0);
            int usedcard = devinfo.path("usedcard").asInt(0);
            int usedpwd = devinfo.path("usedpwd").asInt(0);
            int usedlog = devinfo.path("usedlog").asInt(0);
            int usednewlog = devinfo.path("usednewlog").asInt(0);
            String fpalgo = devinfo.path("fpalgo").asText("");
            String time = devinfo.path("time").asText("");

            System.out.println("Registration received:");
            System.out.println("   SN: " + sn);
            System.out.println("   Model: " + model);
            System.out.println("   Firmware: " + firmware);
            System.out.println("   User capacity: " + usersize);

            // Manejo de BD
            Optional<DeviceModel> existingDeviceOpt = deviceService.getDeviceBySn(sn);
            if (existingDeviceOpt.isPresent()) {
                DeviceModel device = existingDeviceOpt.get();
                deviceService.updateDeviceStatus(device.getId(), DeviceStatus.CONNECTED);
                session.getAttributes().put("sn", sn);
                System.out.println("Existing device marked as CONNECTED: " + sn);
            } else {
                System.out.println("Device not found in DB with SN " + sn + ". Not creating new record.");
            }

            // Tiempo del servidor
            String cloudTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String response = String.format("{\"ret\":\"reg\",\"result\":true,\"cloudtime\":\"%s\"}", cloudTime);
            session.sendMessage(new TextMessage(response));

        } catch (Exception e) {
            e.printStackTrace();
            try {
                session.sendMessage(new TextMessage("{\"ret\":\"reg\", \"result\":false, \"reason\":1}"));
            } catch (Exception ignored) {}
        }
    }
}
