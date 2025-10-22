package com.icm.biometric_zone_gate_api.websocket.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;

/**
 * Envía el comando "setuserlock" al dispositivo.
 * Permite configurar los parámetros de acceso (weekzone, group, horario) para uno o varios usuarios.
 */
@Component
public class SetUserLockCommandSender {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Envía el comando con los registros especificados.
     * @param session Sesión WebSocket del dispositivo.
     * @param userAccessList Lista de mapas con los datos de cada usuario.
     */
    public void sendSetUserLockCommand(WebSocketSession session, List<Map<String, Object>> userAccessList) {
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                    "cmd", "setuserlock",
                    "count", userAccessList.size(),
                    "record", userAccessList
            ));

            System.out.println("📤 Enviando comando SETUSERLOCK al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));

        } catch (Exception e) {
            System.err.println("❌ Error al enviar SETUSERLOCK: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
