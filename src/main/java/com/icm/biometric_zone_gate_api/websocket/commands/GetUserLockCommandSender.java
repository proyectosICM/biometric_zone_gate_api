package com.icm.biometric_zone_gate_api.websocket.commands;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Envía el comando "getuserlock" al dispositivo.
 * Solicita los parámetros de acceso (semana, grupo y validez temporal) de un usuario específico.
 */
@Component
public class GetUserLockCommandSender {

    public void sendGetUserLockCommand(WebSocketSession session, int enrollId) {
        try {
            String message = String.format("{\"cmd\":\"getuserlock\",\"enrollid\":%d}", enrollId);

            System.out.println("📤 Enviando comando GETUSERLOCK al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));

        } catch (Exception e) {
            System.err.println("❌ Error al enviar GETUSERLOCK: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
