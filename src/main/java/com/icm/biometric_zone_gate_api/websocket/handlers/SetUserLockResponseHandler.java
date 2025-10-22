package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * Maneja la respuesta del comando "setuserlock".
 * Indica si el dispositivo aceptó o rechazó la configuración enviada.
 */
@Component
@RequiredArgsConstructor
public class SetUserLockResponseHandler {

    public void handleSetUserLockResponse(JsonNode json, WebSocketSession session) {
        try {
            String ret = json.path("ret").asText("");
            boolean result = json.path("result").asBoolean(false);

            if (!"setuserlock".equalsIgnoreCase(ret)) {
                System.out.println("⚠️ Respuesta ignorada: no corresponde a 'setuserlock'.");
                return;
            }

            if (result) {
                System.out.println("✅ SETUSERLOCK ejecutado correctamente en el dispositivo.");
            } else {
                int reason = json.path("reason").asInt(-1);
                System.err.printf("❌ SETUSERLOCK falló. reason=%d%n", reason);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al procesar respuesta de SETUSERLOCK: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
