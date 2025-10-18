package com.icm.biometric_zone_gate_api.websocket.commands;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class GetAllLogCommandSender {

    public void sendGetAllLogCommand(WebSocketSession session, boolean start) {
        try {
            String message = String.format("{\"cmd\":\"getalllog\",\"stn\":%s}", start ? "true" : "false");
            System.out.println("📤 Enviando comando GETALLLOG al dispositivo...");
            System.out.println(message);
            session.sendMessage(new TextMessage(message));
        } catch (Exception e) {
            System.err.println("❌ Error al enviar GETALLLOG: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
