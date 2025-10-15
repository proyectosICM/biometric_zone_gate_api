package com.icm.biometric_zone_gate_api.websocket.commands;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Envía el comando "deleteuser" al dispositivo.
 * Permite eliminar una huella, password o tarjeta de un usuario.
 */
@Component
public class DeleteUserCommandSender {

    /**
     * Envía el comando deleteuser con los parámetros indicados.
     *
     * @param session WebSocketSession activa con el dispositivo.
     * @param enrollId ID del usuario en el dispositivo.
     * @param backupNum Tipo de credencial (0–9: huella, 10: password, 11: tarjeta RFID).
     */
    public void sendDeleteUserCommand(WebSocketSession session, int enrollId, int backupNum) {
        try {
            String message = String.format(
                    "{\"cmd\":\"deleteuser\",\"enrollid\":%d,\"backupnum\":%d}",
                    enrollId, backupNum
            );

            System.out.println("🗑Enviando comando DELETE USER al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));

        } catch (Exception e) {
            System.err.println("Error al enviar deleteuser: " + e.getMessage());
        }
    }
}
