package com.icm.biometric_zone_gate_api.websocket.commands;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Envía el comando "enableuser" al dispositivo.
 * Permite habilitar o deshabilitar usuarios según su enrollId.
 */
@Component
public class EnableUserCommandSender {

    /**
     * Envía un comando enableuser al dispositivo.
     *
     * @param session WebSocketSession activa con el dispositivo.
     * @param enrollId ID de enrolamiento del usuario.
     * @param enabled true para habilitar, false para deshabilitar.
     */
    public void sendEnableUserCommand(WebSocketSession session, Long enrollId, boolean enabled) {
        try {
            int enflag = enabled ? 1 : 0;

            String message = String.format(
                    "{\"cmd\":\"enableuser\",\"enrollid\":%d,\"enflag\":%d}",
                    enrollId, enflag
            );

            System.out.println("🟢 Enviando comando ENABLE USER al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));

        } catch (Exception e) {
            System.err.println("❌ Error al enviar enableuser: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
