package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Maneja la respuesta del dispositivo al comando "getusername".
 */
@Component
public class GetUserNameResponseHandler {

    public void handleGetUserNameResponse(JsonNode json) {
        try {
            boolean result = json.path("result").asBoolean(false);

            if (result) {
                String record = json.path("record").asText();
                System.out.println("Nombre de usuario obtenido desde el dispositivo: " + record);
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("Falló GET USER NAME. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("Error al procesar respuesta de getusername: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
