package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Maneja la respuesta del dispositivo al comando "setusername".
 */
@Component
@RequiredArgsConstructor
public class SetUserNameResponseHandler {

    public void handleSetUserNameResponse(JsonNode json) {
        try {
            boolean result = json.path("result").asBoolean(false);

            if (result) {
                System.out.println("✅ Dispositivo confirmó SET USERNAME exitoso.");
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("⚠️ Falló SET USERNAME. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al procesar respuesta de setusername: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
