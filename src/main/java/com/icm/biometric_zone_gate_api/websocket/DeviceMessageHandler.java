package com.icm.biometric_zone_gate_api.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class DeviceMessageHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void handle(String message, WebSocketSession session) {
        try {
            System.out.println("📩 Mensaje recibido del dispositivo " + session.getId() + ": " + message);

            JsonNode json = objectMapper.readTree(message);
            String cmd = json.path("cmd").asText("unknown");

            switch (cmd) {

                case "reg" -> handleRegister(json, session);

                case "sendlog" -> {
                    System.out.println("📝 Received logs from " + session.getId());
                    session.sendMessage(new TextMessage("{\"status\": \"ok\", \"msg\": \"Logs received\"}"));
                }

                case "senduser" -> {
                    System.out.println("👤 Received user from " + session.getId());
                    session.sendMessage(new TextMessage("{\"status\": \"ok\", \"msg\": \"User received\"}"));
                }

                default -> {
                    System.out.println("⚠️ Unknown command: " + cmd);
                    session.sendMessage(new TextMessage("{\"status\": \"error\", \"msg\": \"Unknown command\"}"));
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error processing message from " + session.getId() + ": " + e.getMessage());
            e.printStackTrace();
            try {
                session.sendMessage(new TextMessage("{\"ret\":\"reg\", \"result\":false, \"reason\":1}"));
            } catch (Exception ignored) {}
        }
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
            String model = devinfo.path("modelname").asText("");
            String firmware = devinfo.path("firmware").asText("");
            int usersize = devinfo.path("usersize").asInt(0);

            System.out.println("✅ Registro recibido:");
            System.out.println("   SN: " + sn);
            System.out.println("   Modelo: " + model);
            System.out.println("   Firmware: " + firmware);
            System.out.println("   Capacidad usuarios: " + usersize);

            // Aquí luego guardaremos en la BD

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
}
