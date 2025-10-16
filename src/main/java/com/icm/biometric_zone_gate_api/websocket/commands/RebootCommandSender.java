package com.icm.biometric_zone_gate_api.websocket.commands;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Envía el comando "reboot" al dispositivo.
 * Al recibirlo, el dispositivo se reiniciará inmediatamente.
 */
@Component
public class RebootCommandSender {

    /**
     * Envía el comando reboot.
     *
     * @param session WebSocketSession activa con el dispositivo
     */
    public void sendRebootCommand(WebSocketSession session) {
        try {
            String message = "{\"cmd\":\"reboot\"}";

            System.out.println("🟢 Enviando comando REBOOT al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));

        } catch (Exception e) {
            System.err.println("❌ Error al enviar reboot: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
