package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Maneja la respuesta del dispositivo al comando "cleanuser".
 */
@Component
public class CleanUserResponseHandler {

    public void handleCleanUserResponse(JsonNode json) {
        try {
            boolean result = json.path("result").asBoolean(false);

            if (result) {
                System.out.println("✅ Dispositivo confirmó CLEAN USER exitoso.");
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("⚠️ Falló CLEAN USER. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al procesar respuesta de cleanuser: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
