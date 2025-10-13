package com.icm.biometric_zone_gate_api.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import jakarta.websocket.Session;

@Component
public class DeviceMessageHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void handle(String message, Session session) {
        try {
            JsonNode json = objectMapper.readTree(message);
            String cmd = json.get("cmd").asText();

            switch (cmd) {
                case "reg" -> System.out.println("Received register message");
                case "sendlog" -> System.out.println("Received logs");
                case "senduser" -> System.out.println("Received user");
                default -> System.out.println("Unknown command: " + cmd);
            }

        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
    }
}
