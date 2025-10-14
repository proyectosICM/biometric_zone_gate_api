package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class SendUserHandler {

    public void handleSendUser(JsonNode json, WebSocketSession session) {
        try {
            // Leer campos obligatorios
            int enrollId = json.path("enrollid").asInt(-1);
            String name = json.path("name").asText(null);
            int backupNum = json.path("backupnum").asInt(-1);
            int admin = json.path("admin").asInt(0);
            JsonNode recordNode = json.path("record");

            // Validaciones básicas
            if (enrollId <= 0 || name == null || backupNum < 0 || recordNode.isMissingNode()) {
                System.err.println("Invalid user info: missing or invalid fields");
                session.sendMessage(new TextMessage("{\"ret\":\"senduser\",\"result\":false,\"reason\":1}"));
                return;
            }

            // Determinar tipo de registro
            String recordType;
            if (backupNum >= 0 && backupNum <= 9) {
                recordType = "fingerprint";
            } else if (backupNum == 10) {
                recordType = "password";
            } else if (backupNum == 11) {
                recordType = "rfid_card";
            } else {
                recordType = "unknown";
            }

            // Mostrar información del usuario recibido
            System.out.println("Received user info from device:");
            System.out.println(" ├─ enrollid: " + enrollId);
            System.out.println(" ├─ name: " + name);
            System.out.println(" ├─ backupnum: " + backupNum + " (" + recordType + ")");
            System.out.println(" ├─ admin: " + admin);
            System.out.println(" └─ record: " + recordNode.asText());

            // Aquí más adelante podrías guardar el usuario en base de datos
            // vincularlo con el dispositivo según tu modelo de datos

            // Preparar respuesta (éxito)
            String cloudTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String response = String.format(
                    "{\"ret\":\"senduser\",\"result\":true,\"cloudtime\":\"%s\"}",
                    cloudTime
            );

            session.sendMessage(new TextMessage(response));

        } catch (Exception e) {
            e.printStackTrace();
            try {
                session.sendMessage(new TextMessage("{\"ret\":\"senduser\",\"result\":false,\"reason\":1}"));
            } catch (Exception ignored) {}
        }
    }
}
