package com.icm.biometric_zone_gate_api.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class DeviceMessageHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void handle(String message, WebSocketSession session) {
        try {
            JsonNode json = objectMapper.readTree(message);
            String cmd = json.has("cmd") ? json.get("cmd").asText() : "";

            switch (cmd) {
                case "reg" -> System.out.println("Received register message from " + session.getId());
                case "sendlog" -> System.out.println("Received logs from " + session.getId());
                case "senduser" -> System.out.println("Received user from " + session.getId());
                default -> System.out.println("Unknown command: " + cmd);
            }

        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
    }
}
