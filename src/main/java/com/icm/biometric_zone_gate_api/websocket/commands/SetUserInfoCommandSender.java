package com.icm.biometric_zone_gate_api.websocket.commands;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Simula el envío del comando "setuserinfo" al dispositivo.
 * Sirve para descargar un usuario (huella, password o tarjeta RFID).
 */
@Component
public class SetUserInfoCommandSender {

    /**
     * Envía el comando setuserinfo con los parámetros indicados.
     *
     * @param session WebSocketSession activa con el dispositivo.
     * @param enrollId ID del usuario en el dispositivo.
     * @param name Nombre del usuario.
     * @param backupNum Tipo de credencial (0–9: huella, 10: password, 11: RFID).
     * @param admin Nivel de admin (0 usuario normal, 1 admin).
     * @param record Datos del registro (plantilla de huella, contraseña o número de tarjeta).
     */
    public void sendSetUserInfoCommand(WebSocketSession session,
                                       int enrollId,
                                       String name,
                                       int backupNum,
                                       int admin,
                                       String record) {
        try {
            String message = String.format(
                    "{\"cmd\":\"setuserinfo\",\"enrollid\":%d,\"name\":\"%s\",\"backupnum\":%d,\"admin\":%d,\"record\":\"%s\"}",
                    enrollId, name, backupNum, admin, record
            );

            System.out.println("➡️ Enviando comando SET USER INFO al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));

        } catch (Exception e) {
            System.err.println("❌ Error al enviar setuserinfo: " + e.getMessage());
        }
    }
}
