package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.repositories.DeviceUserAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * Maneja la respuesta del dispositivo al comando "deleteuser".
 */
@Component
@RequiredArgsConstructor
public class DeleteUserResponseHandler {

    private final DeviceUserAccessRepository deviceUserAccessRepository;

    public void handleDeleteUserResponse(JsonNode json, WebSocketSession session) {
        try {
            String ret = json.path("ret").asText("");
            boolean result = json.path("result").asBoolean(false);

            if (!"deleteuser".equalsIgnoreCase(ret)) {
                return;
            }

            // ✅ Recuperar SN del dispositivo
            String sn = (String) session.getAttributes().get("sn");
            if (sn == null) {
                System.err.println("⚠ No se puede asociar ACK deleteuser: SN no encontrado en sesión");
                return;
            }

            int enrollId = json.path("enrollid").asInt(-1);

            if (result) {
                // ✅ ACK DELETE => eliminación física definitiva
                deviceUserAccessRepository.findByDeviceSnAndEnrollIdAndPendingDeleteTrue(sn, enrollId)
                        .ifPresent(access -> {
                            deviceUserAccessRepository.delete(access);
                            System.out.printf("🗑️ Acceso enrollId=%d eliminado DEFINITIVAMENTE%n", enrollId);
                        });
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("Falló DELETE USER. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("Error al procesar respuesta de deleteuser: " + e.getMessage());
        }
    }
}
