package com.icm.biometric_zone_gate_api.websocket.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Envía el comando "opendoor" al dispositivo.
 * Este comando solicita al terminal abrir la puerta (activar el relay).
 */
@Component
public class OpenDoorCommandSender {

    private final ObjectMapper mapper = new ObjectMapper();

    public void sendOpenDoorCommand(WebSocketSession session) {
        try {
            String message = "{\"cmd\":\"opendoor\"}";

            System.out.println("Enviando comando OPENDOOR al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));

        } catch (Exception e) {
            System.err.println("Error al enviar OPENDOOR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Innecesario pero implementado por las dudas */
    public void sendOpenDoorCommand(WebSocketSession session, Integer doorNum) {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("cmd", "opendoor");
            if (doorNum != null) root.put("doornum", doorNum); // si null → abre todas
            String message = mapper.writeValueAsString(root);

            System.out.println("Enviando comando OPENDOOR al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));
        } catch (Exception e) {
            System.err.println("Error al enviar OPENDOOR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
