package com.icm.biometric_zone_gate_api.websocket.commands;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Envía el comando "settime" al dispositivo.
 * Este comando sincroniza la hora del terminal con la hora enviada por el servidor.
 */
@Component
public class SetTimeCommandSender {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Envía el comando "settime" con la hora actual del servidor.
     *
     * @param session sesión WebSocket activa con el dispositivo
     */
    public void sendSetTimeCommand(WebSocketSession session) {
        try {
            String cloudTime = LocalDateTime.now().format(FORMATTER);
            String message = String.format("{\"cmd\":\"settime\",\"cloudtime\":\"%s\"}", cloudTime);

            System.out.println("⏰ Enviando comando SETTIME al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));

        } catch (Exception e) {
            System.err.println("❌ Error al enviar settime: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Envía el comando "settime" con una hora personalizada.
     *
     * @param session sesión WebSocket activa con el dispositivo
     * @param dateTime hora personalizada (por ejemplo, desde la API)
     */
    public void sendSetTimeCommand(WebSocketSession session, LocalDateTime dateTime) {
        try {
            String cloudTime = dateTime.format(FORMATTER);
            String message = String.format("{\"cmd\":\"settime\",\"cloudtime\":\"%s\"}", cloudTime);

            System.out.println("⏰ Enviando comando SETTIME personalizado al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));

        } catch (Exception e) {
            System.err.println("❌ Error al enviar settime personalizado: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
