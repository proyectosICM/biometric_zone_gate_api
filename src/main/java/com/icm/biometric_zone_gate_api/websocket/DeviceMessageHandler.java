package com.icm.biometric_zone_gate_api.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icm.biometric_zone_gate_api.enums.DeviceStatus;
import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.services.DeviceService;
import com.icm.biometric_zone_gate_api.websocket.handlers.RegisterHandler;
import com.icm.biometric_zone_gate_api.websocket.utils.DeviceValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DeviceMessageHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DeviceService deviceService;

    private final RegisterHandler registerHandler;

    public void handle(String message, WebSocketSession session) {
        try {
            System.out.println("Mensaje recibido del dispositivo " + session.getId() + ": " + message);

            JsonNode json = objectMapper.readTree(message);
            String cmd = json.path("cmd").asText("unknown");

            switch (cmd) {

                case "reg" -> registerHandler.handleRegister(json, session);

                case "sendlog" -> handleSendLog(json, session);

                case "senduser" -> {
                    System.out.println("Received user from " + session.getId());
                    session.sendMessage(new TextMessage("{\"status\": \"ok\", \"msg\": \"User received\"}"));
                }

                default -> {
                    System.out.println("Unknown command: " + cmd);
                    session.sendMessage(new TextMessage("{\"status\": \"error\", \"msg\": \"Unknown command\"}"));
                }
            }

        } catch (Exception e) {
            System.err.println("Error processing message from " + session.getId() + ": " + e.getMessage());
            e.printStackTrace();
            try {
                session.sendMessage(new TextMessage("{\"ret\":\"reg\", \"result\":false, \"reason\":1}"));
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        System.out.println("Device disconnected: " + session.getId());

        // Recuperar SN guardado en la sesión
        String sn = (String) session.getAttributes().get("sn");
        if (sn != null) {
            deviceService.getDeviceBySn(sn).ifPresent(device -> {
                deviceService.updateDeviceStatus(device.getId(), DeviceStatus.DISCONNECTED);
                System.out.println("Dispositivo marcado como DESCONECTADO: " + sn);
            });
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("Device connected: " + session.getId());
        // SN se asignará en handleRegister cuando llegue el mensaje
    }

    private void handleRegister(JsonNode json, WebSocketSession session) {
        try {
            String sn = json.path("sn").asText(null);
            if (sn == null || sn.isEmpty()) {
                System.err.println("Registro inválido: falta SN");
                session.sendMessage(new TextMessage("{\"ret\":\"reg\", \"result\":false, \"reason\":1}"));
                return;
            }

            // Extraer información del devinfo
            JsonNode devinfo = json.path("devinfo");

            // Validar devinfo completo
            if (!DeviceValidator.validateDevInfo(devinfo)) {
                System.err.println("Registro inválido: devinfo incompleto o incorrecto");
                session.sendMessage(new TextMessage("{\"ret\":\"reg\", \"result\":false, \"reason\":1}"));
                return;
            }

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

            System.out.println("Registro recibido:");
            System.out.println("   SN: " + sn);
            System.out.println("   Modelo: " + model);
            System.out.println("   Firmware: " + firmware);
            System.out.println("   Capacidad usuarios: " + usersize);

            // Aquí luego guardaremos en la BD
            Optional<DeviceModel> existingDeviceOpt = deviceService.getDeviceBySn(sn);
            if (existingDeviceOpt.isPresent()) {
                DeviceModel device = existingDeviceOpt.get();

                // Cambiar estado a CONECTADO
                deviceService.updateDeviceStatus(device.getId(), DeviceStatus.CONNECTED);

                // Guardar el SN en los atributos de la sesión para poder marcarlo desconectado después
                session.getAttributes().put("sn", sn);

                System.out.println("Dispositivo existente marcado como CONECTADO: " + sn);
            } else {
                System.out.println("Dispositivo no encontrado en BD con SN " + sn + ". No se creará nuevo registro.");
            }

            // Formatear hora actual del servidor
            String cloudTime = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // Enviar respuesta de éxito
            String response = String.format("{\"ret\":\"reg\",\"result\":true,\"cloudtime\":\"%s\"}", cloudTime);
            session.sendMessage(new TextMessage(response));

        } catch (Exception e) {
            e.printStackTrace();
            try {
                session.sendMessage(new TextMessage("{\"ret\":\"reg\", \"result\":false, \"reason\":1}"));
            } catch (Exception ignored) {}
        }
    }


    private void handleSendLog(JsonNode json, WebSocketSession session) {
        try {
            int count = json.path("count").asInt(0);
            JsonNode records = json.path("record");

            if (count <= 0 || !records.isArray() || records.size() != count) {
                System.err.println("Logs inválidos: count no coincide con records");
                session.sendMessage(new TextMessage("{\"ret\":\"sendlog\", \"result\":false, \"reason\":1}"));
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
            }

            // Hora del servidor
            String cloudTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // Simular access = 1 (puerta abierta)
            String response = String.format("{\"ret\":\"sendlog\",\"result\":true,\"cloudtime\":\"%s\",\"access\":1}", cloudTime);
            session.sendMessage(new TextMessage(response));

        } catch (Exception e) {
            e.printStackTrace();
            try {
                session.sendMessage(new TextMessage("{\"ret\":\"sendlog\", \"result\":false, \"reason\":1}"));
            } catch (Exception ignored) {}
        }
    }

}
