package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * Maneja la respuesta del comando "cleanuserlock".
 * Indica si se limpiaron correctamente todos los parámetros de acceso.
 */
@Component
@RequiredArgsConstructor
public class CleanUserLockResponseHandler {

    public void handleCleanUserLockResponse(JsonNode json, WebSocketSession session) {
        try {
            String ret = json.path("ret").asText("");
            boolean result = json.path("result").asBoolean(false);

            if (!"cleanuserlock".equalsIgnoreCase(ret)) {
                System.out.println("⚠️ Respuesta ignorada: no corresponde a 'cleanuserlock'.");
                return;
            }

            if (result) {
                System.out.println("✅ CLEANUSERLOCK ejecutado correctamente: todos los parámetros de acceso fueron eliminados.");
            } else {
                int reason = json.path("reason").asInt(-1);
                System.err.printf("❌ CLEANUSERLOCK falló. reason=%d%n", reason);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al procesar respuesta de CLEANUSERLOCK: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
