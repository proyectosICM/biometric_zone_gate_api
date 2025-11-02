package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.CleanLogDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * Maneja la respuesta del dispositivo al comando "cleanlog".
 */
@Component
@RequiredArgsConstructor
public class CleanLogResponseHandler {
    private final CleanLogDispatcher dispatcher;
    private final DeviceRepository deviceRepository;

    public void handleCleanLogResponse(JsonNode json, WebSocketSession session) {
        try {
            boolean result = json.path("result").asBoolean(false);
            String sn = (String) session.getAttributes().get("sn");

            if (result) {
                System.out.println("Dispositivo confirmó CLEANLOG exitoso (registros eliminados).");
                if (sn != null) {
                    dispatcher.confirm(sn);
                    /*
                    deviceRepository.findBySn(sn).ifPresent(dev -> {
                        dev.setPendingClean(false);
                        deviceRepository.save(dev);
                    });
                     */
                }
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("Falló CLEANLOG. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("Error al procesar respuesta de CLEANLOG: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
