package com.icm.biometric_zone_gate_api.websocket.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

/**
 * Envía el comando "cleanuserlock" al dispositivo.
 * Limpia todos los parámetros de acceso de todos los usuarios.
 */
@Component
public class CleanUserLockCommandSender {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Envía el comando para limpiar todos los parámetros de acceso de usuario.
     * @param session Sesión WebSocket con el dispositivo.
     */
    public void sendCleanUserLockCommand(WebSocketSession session) {
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                    "cmd", "cleanuserlock"
            ));

            System.out.println("📤 Enviando comando CLEANUSERLOCK al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));

        } catch (Exception e) {
            System.err.println("❌ Error al enviar CLEANUSERLOCK: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
