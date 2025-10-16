package com.icm.biometric_zone_gate_api.websocket.commands;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Envía el comando "cleanadmin" al dispositivo.
 * Este comando convierte todos los administradores en usuarios normales.
 */
@Component
public class CleanAdminCommandSender {

    /**
     * Envía el comando cleanadmin al dispositivo.
     *
     * @param session WebSocketSession activa con el dispositivo.
     */
    public void sendCleanAdminCommand(WebSocketSession session) {
        try {
            String message = "{\"cmd\":\"cleanadmin\"}";

            System.out.println("🧹 Enviando comando CLEAN ADMIN al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));

        } catch (Exception e) {
            System.err.println("❌ Error al enviar cleanadmin: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
