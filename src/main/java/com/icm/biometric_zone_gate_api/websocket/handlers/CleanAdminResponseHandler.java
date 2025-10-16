package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Maneja la respuesta del dispositivo al comando "cleanadmin".
 * Este comando convierte todos los administradores en usuarios normales.
 */
@Component
public class CleanAdminResponseHandler {

    public void handleCleanAdminResponse(JsonNode json) {
        try {
            boolean result = json.path("result").asBoolean(false);

            if (result) {
                System.out.println("✅ Dispositivo confirmó CLEAN ADMIN exitoso (todos los admins ahora son usuarios normales).");
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("⚠️ Falló CLEAN ADMIN. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al procesar respuesta de cleanadmin: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
