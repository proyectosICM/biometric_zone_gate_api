package com.icm.biometric_zone_gate_api.websocket.commands;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Envía el comando "getdevlock" al dispositivo para obtener la configuración actual de acceso.
 */
@Component
public class GetDevLockCommandSender {

    public void sendGetDevLockCommand(WebSocketSession session) {
        try {
            String message = """
                {
                    "cmd": "getdevlock"
                }
                """;

            System.out.println("📤 Enviando comando GETDEVLOCK al dispositivo...");
            session.sendMessage(new TextMessage(message));

        } catch (Exception e) {
            System.err.println("❌ Error al enviar GETDEVLOCK: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
