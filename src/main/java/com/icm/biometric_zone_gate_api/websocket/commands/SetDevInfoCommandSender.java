package com.icm.biometric_zone_gate_api.websocket.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Envía el comando "setdevinfo" al dispositivo.
 * Sirve para actualizar los parámetros del terminal (idioma, volumen, etc.).
 */
@Component
public class SetDevInfoCommandSender {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Envía un payload ya armado (solo las claves presentes serán aplicadas). */
    public void sendSetDevInfoCommand(WebSocketSession session, ObjectNode payload) {
        try {
            if (!payload.has("cmd")) payload.put("cmd", "setdevinfo");
            String message = objectMapper.writeValueAsString(payload);

            System.out.println("Enviando comando SETDEVINFO al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));
        } catch (Exception e) {
            System.err.println("Error al enviar SETDEVINFO: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Envía al dispositivo los parámetros recibidos (normalmente obtenidos desde getdevinfo).
     */
    public void sendSetDevInfoCommand(WebSocketSession session,
                                      int deviceId,
                                      int language,
                                      int volume,
                                      int screensaver,
                                      int verifymode,
                                      int sleep,
                                      int userfpnum,
                                      int loghint,
                                      int reverifytime) {
        try {
            ObjectNode json = objectMapper.createObjectNode();
            json.put("cmd", "setdevinfo");
            json.put("deviceid", deviceId);
            json.put("language", language);
            json.put("volume", volume);
            json.put("screensaver", screensaver);
            json.put("verifymode", verifymode);
            json.put("sleep", sleep);
            json.put("userfpnum", userfpnum);
            json.put("loghint", loghint);
            json.put("reverifytime", reverifytime);

            String message = objectMapper.writeValueAsString(json);

            System.out.println("Enviando comando SETDEVINFO al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));

        } catch (Exception e) {
            System.err.println("Error al enviar SETDEVINFO: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
