package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.SetTimeDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * Maneja la respuesta del dispositivo al comando "settime".
 * Indica si el dispositivo pudo sincronizar correctamente su hora.
 */
@Component
@RequiredArgsConstructor
public class SetTimeResponseHandler {

    private final SetTimeDispatcher dispatcher;

    public void handleSetTimeResponse(JsonNode json, WebSocketSession session) {
        try {
            boolean result = json.path("result").asBoolean(false);
            String sn = (String) session.getAttributes().get("sn");

            if (result) {
                System.out.println("Dispositivo confirmó SETTIME exitoso (hora actualizada correctamente).");
                if (sn != null) dispatcher.confirm(sn);
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("Falló SETTIME. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("Error al procesar respuesta de settime: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
