package com.icm.biometric_zone_gate_api.websocket.commands;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Envía el comando "opendoor" al dispositivo.
 * Este comando solicita al terminal abrir la puerta (activar el relay).
 */
@Component
public class OpenDoorCommandSender {

    public void sendOpenDoorCommand(WebSocketSession session) {
        try {
            String message = "{\"cmd\":\"opendoor\"}";

            System.out.println("🔓 Enviando comando OPENDOOR al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));

        } catch (Exception e) {
            System.err.println("❌ Error al enviar OPENDOOR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
