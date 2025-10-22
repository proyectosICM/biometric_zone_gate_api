package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Maneja la respuesta del comando "setdevlock" enviado al dispositivo.
 * Solo valida y registra el resultado.
 */
@Component
public class SetDevLockResponseHandler {

    public void handleSetDevLockResponse(JsonNode json) {
        try {
            boolean result = json.path("result").asBoolean(false);
            String ret = json.path("ret").asText("");

            if (!"setdevlock".equalsIgnoreCase(ret)) {
                System.out.println("⚠️ Respuesta ignorada: no corresponde a 'setdevlock'.");
                return;
            }

            if (result) {
                System.out.println("✅ SETDEVLOCK aplicado correctamente en el dispositivo.");
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.printf("❌ Falló SETDEVLOCK. reason=%d%n", reason);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al procesar respuesta de SETDEVLOCK: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
