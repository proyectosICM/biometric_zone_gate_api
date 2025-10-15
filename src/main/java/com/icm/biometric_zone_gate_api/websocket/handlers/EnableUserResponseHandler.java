package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Maneja la respuesta del dispositivo al comando "enableuser".
 * Este mismo comando sirve para habilitar y deshabilitar usuarios.
 */
@Component
public class EnableUserResponseHandler {

    public void handleEnableUserResponse(JsonNode json) {
        try {
            boolean result = json.path("result").asBoolean(false);

            if (result) {
                System.out.println("✅ Dispositivo confirmó ENABLE/DISABLE USER exitoso.");
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("⚠️ Falló ENABLE/DISABLE USER. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al procesar respuesta de enableuser: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
