package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Maneja la respuesta del dispositivo al comando "opendoor".
 */
@Component
public class OpenDoorResponseHandler {

    public void handleOpenDoorResponse(JsonNode json) {
        try {
            String ret = json.path("ret").asText("");
            boolean result = json.path("result").asBoolean(false);

            if (!"opendoor".equalsIgnoreCase(ret)) return;

            if (result) {
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
