package com.icm.biometric_zone_gate_api.websocket.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class GetUserListCommandSender {

    /**
     * Send the command to the device to get the list of users.
     * @param session WebSocketSession active with the device
     * @param stn true if this is the first request in the batch
     */
    public void sendGetUserListCommand(WebSocketSession session, boolean stn) {
        try {
            String message = String.format("{\"cmd\":\"getuserlist\",\"stn\":%s}", stn ? "true" : "false");

            System.out.println("Enviando comando GET USER LIST al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));
        } catch (Exception e) {
            System.err.println("Error al enviar getuserlist: " + e.getMessage());
        }
    }
}
