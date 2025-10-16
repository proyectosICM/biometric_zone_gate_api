package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Maneja la respuesta del dispositivo al comando "cleanlog".
 */
@Component
public class CleanLogResponseHandler {

    public void handleCleanLogResponse(JsonNode json) {
        try {
            boolean result = json.path("result").asBoolean(false);

            if (result) {
                System.out.println("✅ Dispositivo confirmó CLEANLOG exitoso (registros eliminados).");
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("⚠️ Falló CLEANLOG. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al procesar respuesta de CLEANLOG: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
