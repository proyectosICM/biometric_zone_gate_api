package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Maneja la respuesta del dispositivo al comando "setuserinfo".
 */
@Component
@RequiredArgsConstructor
public class SetUserInfoResponseHandler {

    public void handleSetUserInfoResponse(JsonNode json) {
        try {
            boolean result = json.path("result").asBoolean(false);

            if (result) {
                System.out.println("✅ Dispositivo confirmó SET USER INFO exitoso.");
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("⚠️ Falló SET USER INFO. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al procesar respuesta de setuserinfo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
