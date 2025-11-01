package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.OpenDoorDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * Maneja la respuesta del dispositivo al comando "opendoor".
 */
@Component
@RequiredArgsConstructor
public class OpenDoorResponseHandler {

    private final OpenDoorDispatcher dispatcher;

    public void handleOpenDoorResponse(JsonNode json, WebSocketSession session) {
        try {
            String ret = json.path("ret").asText("");
            boolean result = json.path("result").asBoolean(false);

            if (!"opendoor".equalsIgnoreCase(ret)) return;
            String sn = (String) session.getAttributes().get("sn");

            if (sn == null) {
                System.err.println("⚠ No SN en sesión para ACK opendoor");
                return;
            }

            if (result) {
                dispatcher.ack(sn);
                System.out.println("Dispositivo confirmó OPENDOOR exitoso.");
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("Falló OPENDOOR. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("Error al procesar respuesta OPENDOOR: " + e.getMessage());
        }
    }


}
