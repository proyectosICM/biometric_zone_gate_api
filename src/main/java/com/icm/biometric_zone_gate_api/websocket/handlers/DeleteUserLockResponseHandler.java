package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * Maneja la respuesta del comando "deleteuserlock".
 * Indica si el dispositivo eliminó correctamente el parámetro de acceso del usuario.
 */
@Component
@RequiredArgsConstructor
public class DeleteUserLockResponseHandler {

    public void handleDeleteUserLockResponse(JsonNode json, WebSocketSession session) {
        try {
            String ret = json.path("ret").asText("");
            boolean result = json.path("result").asBoolean(false);

            if (!"deleteuserlock".equalsIgnoreCase(ret)) {
                System.out.println("⚠️ Respuesta ignorada: no corresponde a 'deleteuserlock'.");
                return;
            }

            if (result) {
                System.out.println("✅ DELETEUSERLOCK ejecutado correctamente: el usuario fue eliminado del dispositivo.");
            } else {
                int reason = json.path("reason").asInt(-1);
                System.err.printf("❌ DELETEUSERLOCK falló. reason=%d%n", reason);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al procesar respuesta de DELETEUSERLOCK: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
