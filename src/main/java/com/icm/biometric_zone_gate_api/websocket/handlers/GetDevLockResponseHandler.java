package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Maneja la respuesta del comando "getdevlock" del dispositivo.
 * Solo muestra los parámetros recibidos por consola.
 */
@Component
public class GetDevLockResponseHandler {

    public void handleGetDevLockResponse(JsonNode json) {
        try {
            String ret = json.path("ret").asText("");
            boolean result = json.path("result").asBoolean(false);

            if (!"getdevlock".equalsIgnoreCase(ret)) {
                System.out.println("⚠️ Respuesta ignorada: no corresponde a 'getdevlock'.");
                return;
            }

            if (!result) {
                int reason = json.path("reason").asInt(-1);
                System.out.printf("❌ Falló GETDEVLOCK. reason=%d%n", reason);
                return;
            }

            // ✅ Mostrar configuración recibida
            System.out.println("✅ GETDEVLOCK recibido correctamente:");
            System.out.printf(
                    """
                    ───────────────
                    openDelay: %d
                    doorSensor: %d
                    alarmDelay: %d
                    threat: %d
                    InputAlarm: %d
                    antpass: %d
                    interlock: %d
                    mutiopen: %d
                    tryalarm: %d
                    tamper: %d
                    wgformat: %d
                    wgoutput: %d
                    cardoutput: %d
                    ───────────────
                    """,
                    json.path("opendelay").asInt(),
                    json.path("doorsensor").asInt(),
                    json.path("alarmdelay").asInt(),
                    json.path("threat").asInt(),
                    json.path("InputAlarm").asInt(),
                    json.path("antpass").asInt(),
                    json.path("interlock").asInt(),
                    json.path("mutiopen").asInt(),
                    json.path("tryalarm").asInt(),
                    json.path("tamper").asInt(),
                    json.path("wgformat").asInt(),
                    json.path("wgoutput").asInt(),
                    json.path("cardoutput").asInt()
            );

            // Si queremos ver dayzone, weekzone y lockgroup (solo para debug):
            if (json.has("dayzone")) {
                System.out.println("📅 DayZone: " + json.get("dayzone").toPrettyString());
            }
            if (json.has("weekzone")) {
                System.out.println("📆 WeekZone: " + json.get("weekzone").toPrettyString());
            }
            if (json.has("lockgroup")) {
                System.out.println("🔐 LockGroup: " + json.get("lockgroup").toPrettyString());
            }

        } catch (Exception e) {
            System.err.println("❌ Error al procesar respuesta GETDEVLOCK: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
