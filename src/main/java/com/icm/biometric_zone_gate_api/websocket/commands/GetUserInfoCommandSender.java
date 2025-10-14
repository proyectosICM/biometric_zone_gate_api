package com.icm.biometric_zone_gate_api.websocket.commands;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class GetUserInfoCommandSender {

    /**
     * Send the getuserinfo command to the device.
     * @param session WebSocketSession active with the device
     * @param enrollId ID del usuario
     * @param backupNum Tipo de credencial (0-9 huella, 10 password, 11 RFID)
     */
    public void sendGetUserInfoCommand(WebSocketSession session, int enrollId, int backupNum) {
        try {
            String message = String.format(
                    "{\"cmd\":\"getuserinfo\",\"enrollid\":%d,\"backupnum\":%d}",
                    enrollId, backupNum
            );

            System.out.println("Enviando comando GET USER INFO al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));
        } catch (Exception e) {
            System.err.println("Error al enviar getuserinfo: " + e.getMessage());
        }
    }
}
