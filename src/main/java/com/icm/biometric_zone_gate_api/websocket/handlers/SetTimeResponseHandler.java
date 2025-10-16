package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Maneja la respuesta del dispositivo al comando "settime".
 * Indica si el dispositivo pudo sincronizar correctamente su hora.
 */
@Component
public class SetTimeResponseHandler {

    public void handleSetTimeResponse(JsonNode json) {
        try {
            boolean result = json.path("result").asBoolean(false);

            if (result) {
                System.out.println("✅ Dispositivo confirmó SETTIME exitoso (hora actualizada correctamente).");
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("⚠️ Falló SETTIME. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al procesar respuesta de settime: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
