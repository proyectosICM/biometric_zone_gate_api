package com.icm.biometric_zone_gate_api.websocket.commands;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Envía el comando "getnewlog" al dispositivo.
 * Este comando solicita los registros nuevos en el terminal.
 *
 * Parámetro "stn":
 *  - true → primera solicitud (inicio)
 *  - false → solicitud de continuación (siguiente paquete)
 */
@Component
public class GetNewLogCommandSender {

    public void sendGetNewLogCommand(WebSocketSession session, boolean start) {
        try {
            String message = String.format("{\"cmd\":\"getnewlog\",\"stn\":%s}", start ? "true" : "false");

            System.out.println("Enviando comando GETNEWLOG al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));

        } catch (Exception e) {
            System.err.println("Error al enviar GETNEWLOG: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
