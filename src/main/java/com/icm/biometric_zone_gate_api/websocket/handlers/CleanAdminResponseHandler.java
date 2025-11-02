package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.CleanAdminDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * Maneja la respuesta del dispositivo al comando "cleanadmin".
 * Este comando convierte todos los administradores en usuarios normales.
 */
@Component
@RequiredArgsConstructor
public class CleanAdminResponseHandler {

    private final CleanAdminDispatcher dispatcher;
    private final DeviceRepository deviceRepository;

    public void handleCleanAdminResponse(JsonNode json, WebSocketSession session) {
        try {
            boolean result = json.path("result").asBoolean(false);
            String sn = (String) session.getAttributes().get("sn");

            if (result) {
                System.out.println("Dispositivo confirmó CLEAN ADMIN exitoso (todos los admins ahora son usuarios normales).");
                if (sn != null) {
                    dispatcher.confirm(sn);
                }
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("Falló CLEAN ADMIN. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("Error al procesar respuesta de cleanadmin: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
