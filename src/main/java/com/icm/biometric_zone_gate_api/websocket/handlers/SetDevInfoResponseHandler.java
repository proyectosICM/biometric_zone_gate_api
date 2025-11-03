package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.SetDevInfoDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * Maneja la respuesta del dispositivo al comando "setdevinfo".
 */
@Component
@RequiredArgsConstructor
public class SetDevInfoResponseHandler {

    private final SetDevInfoDispatcher setDevInfoDispatcher;
    private final DeviceRepository deviceRepository;

    public void handleSetDevInfoResponse(JsonNode json, WebSocketSession session) {
        try {
            boolean result = json.path("result").asBoolean(false);
            String sn = (String) session.getAttributes().get("sn");

            if (result) {
                System.out.println("SETDEVINFO ejecutado correctamente en el dispositivo.");
                if (sn != null) setDevInfoDispatcher.ack(sn);
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("Falló SETDEVINFO. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("Error al procesar respuesta de SETDEVINFO: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
