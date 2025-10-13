package com.icm.biometric_zone_gate_api.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class DeviceMessageHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void handle(String message, WebSocketSession session) {
        try {
            System.out.println("📩 Mensaje recibido del dispositivo " + session.getId() + ": " + message);

            JsonNode json = objectMapper.readTree(message);
            String cmd = json.has("cmd") ? json.get("cmd").asText() : "unknown";

            switch (cmd) {
                case "reg" -> {
                    System.out.println("✅ Received register message from " + session.getId());
                    // 🔹 Enviar respuesta al cliente Python
                    session.sendMessage(new TextMessage("{\"status\": \"ok\", \"msg\": \"Device registered\"}"));
                }
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
                session.sendMessage(new TextMessage("{\"status\": \"error\", \"msg\": \"Exception occurred\"}"));
            } catch (Exception ignored) {}
        }
    }
}
