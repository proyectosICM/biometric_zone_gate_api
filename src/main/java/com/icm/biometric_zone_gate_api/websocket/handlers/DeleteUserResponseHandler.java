package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.repositories.DeviceUserAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Maneja la respuesta del dispositivo al comando "deleteuser".
 */
@Component
@RequiredArgsConstructor
public class DeleteUserResponseHandler {

    private final DeviceUserAccessRepository deviceUserAccessRepository;

    public void handleDeleteUserResponse(JsonNode json) {
        try {
            String ret = json.path("ret").asText("");
            boolean result = json.path("result").asBoolean(false);

            if (!"deleteuser".equalsIgnoreCase(ret)) {
                return;
            }

            int enrollId = json.path("enrollid").asInt(-1);

            if (result) {
                // ✅ ACK DELETE => eliminación física definitiva
                deviceUserAccessRepository.findByEnrollIdAndPendingDeleteTrue(enrollId)
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
