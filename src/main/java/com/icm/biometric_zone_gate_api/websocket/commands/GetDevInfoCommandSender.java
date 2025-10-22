package com.icm.biometric_zone_gate_api.websocket.commands;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Envía el comando "getdevinfo" al dispositivo.
 * Este comando solicita al terminal enviar sus parámetros generales
 * (idioma, volumen, modo de verificación, etc.).
 */
@Component
public class GetDevInfoCommandSender {

    /**
     * Envía el comando GETDEVINFO a un dispositivo conectado.
     *
     * @param session Sesión WebSocket asociada al dispositivo.
     */
    public void sendGetDevInfoCommand(WebSocketSession session) {
        try {
            String message = "{\"cmd\":\"getdevinfo\"}";

            System.out.println("Enviando comando GETDEVINFO al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));

        } catch (Exception e) {
            System.err.println("Error al enviar GETDEVINFO: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
