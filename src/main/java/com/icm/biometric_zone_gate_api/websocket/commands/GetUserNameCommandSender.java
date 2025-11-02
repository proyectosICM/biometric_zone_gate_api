package com.icm.biometric_zone_gate_api.websocket.commands;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Envía el comando "getusername" al dispositivo.
 * Solicita el nombre de un usuario a partir de su enrollId.
 */
@Component
public class GetUserNameCommandSender {

    /**
     * Envía el comando getusername con el enrollId indicado.
     *
     * @param session WebSocketSession activa con el dispositivo.
     * @param enrollId ID del usuario a consultar.
     */
    public void sendGetUserNameCommand(WebSocketSession session, int enrollId) {
        try {
            String message = String.format(
                    "{\"cmd\":\"getusername\",\"enrollid\":%d}",
                    enrollId
            );

            System.out.println("Enviando comando GET USER NAME al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));

        } catch (Exception e) {
            System.err.println("❌ Error al enviar getusername: " + e.getMessage());
        }
    }
}
