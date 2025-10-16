package com.icm.biometric_zone_gate_api.websocket.commands;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Envía el comando "cleanuser" al dispositivo.
 * Borra todos los usuarios almacenados en el dispositivo.
 */
@Component
public class CleanUserCommandSender {

    /**
     * Envía el comando cleanuser.
     *
     * @param session WebSocketSession activa con el dispositivo
     */
    public void sendCleanUserCommand(WebSocketSession session) {
        try {
            String message = "{\"cmd\":\"cleanuser\"}";

            System.out.println("🟢 Enviando comando CLEAN USER al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));

        } catch (Exception e) {
            System.err.println("❌ Error al enviar cleanuser: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
