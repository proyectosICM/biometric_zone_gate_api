package com.icm.biometric_zone_gate_api.websocket.commands;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Envía el comando "initsys" al dispositivo.
 * Este comando borra todos los usuarios y logs del sistema,
 * pero mantiene las configuraciones del dispositivo.
 */
@Component
public class InitSystemCommandSender {

    /**
     * Envía el comando initsys.
     *
     * @param session WebSocketSession activa con el dispositivo
     */
    public void sendInitSystemCommand(WebSocketSession session) {
        try {
            String message = "{\"cmd\":\"initsys\"}";

            System.out.println("🟢 Enviando comando INIT SYSTEM al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));

        } catch (Exception e) {
            System.err.println("❌ Error al enviar initsys: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
