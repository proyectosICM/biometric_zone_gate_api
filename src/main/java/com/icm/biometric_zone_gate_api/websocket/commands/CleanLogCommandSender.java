package com.icm.biometric_zone_gate_api.websocket.commands;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Envía el comando "cleanlog" al dispositivo.
 * Este comando solicita eliminar todos los registros del terminal.
 */
@Component
public class CleanLogCommandSender {

    public void sendCleanLogCommand(WebSocketSession session) {
        try {
            String message = "{\"cmd\":\"cleanlog\"}";

            System.out.println("🧹 Enviando comando CLEANLOG al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));

        } catch (Exception e) {
            System.err.println("❌ Error al enviar CLEANLOG: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
