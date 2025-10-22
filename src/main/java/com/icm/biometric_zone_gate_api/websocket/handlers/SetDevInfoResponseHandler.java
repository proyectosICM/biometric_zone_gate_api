package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Maneja la respuesta del dispositivo al comando "setdevinfo".
 */
@Component
public class SetDevInfoResponseHandler {

    public void handleSetDevInfoResponse(JsonNode json) {
        try {
            boolean result = json.path("result").asBoolean(false);

            if (result) {
                System.out.println("✅ SETDEVINFO ejecutado correctamente en el dispositivo.");
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("⚠️ Falló SETDEVINFO. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al procesar respuesta de SETDEVINFO: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
