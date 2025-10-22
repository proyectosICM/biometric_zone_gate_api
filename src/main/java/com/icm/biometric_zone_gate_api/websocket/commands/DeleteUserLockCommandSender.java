package com.icm.biometric_zone_gate_api.websocket.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

/**
 * Envía el comando "deleteuserlock" al dispositivo.
 * Elimina los parámetros de acceso de un usuario específico (por enrollid).
 */
@Component
public class DeleteUserLockCommandSender {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Envía el comando de borrado de parámetros de acceso de un usuario.
     * @param session Sesión WebSocket con el dispositivo.
     * @param enrollId ID de usuario a eliminar.
     */
    public void sendDeleteUserLockCommand(WebSocketSession session, int enrollId) {
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                    "cmd", "deleteuserlock",
                    "enrollid", enrollId
            ));

            System.out.println("📤 Enviando comando DELETEUSERLOCK al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));

        } catch (Exception e) {
            System.err.println("❌ Error al enviar DELETEUSERLOCK: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
