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
            boolean result = json.path("result").asBoolean(false);

            if (result) {
                System.out.println("✅ Dispositivo confirmó OPENDOOR exitoso (puerta abierta).");
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("⚠️ Falló OPENDOOR. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al procesar respuesta de OPENDOOR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
