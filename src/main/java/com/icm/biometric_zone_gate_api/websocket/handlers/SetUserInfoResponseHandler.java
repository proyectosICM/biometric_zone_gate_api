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
            String ret = json.path("ret").asText("");
            boolean result = json.path("result").asBoolean(false);

            if (!"setuserinfo".equalsIgnoreCase(ret)) {
                return;
            }

            if (result) {
                System.out.println("Dispositivo confirmó SETUSERINFO (descarga OK)");
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("Dispositivo rechazó SETUSERINFO. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("Error al procesar respuesta de setuserinfo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
