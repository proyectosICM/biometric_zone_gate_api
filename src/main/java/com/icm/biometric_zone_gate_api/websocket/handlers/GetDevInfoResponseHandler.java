package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Maneja la respuesta del dispositivo al comando "getdevinfo".
 */
@Component
public class GetDevInfoResponseHandler {

    public void handleGetDevInfoResponse(JsonNode json) {
        try {
            boolean result = json.path("result").asBoolean(false);

            if (result) {
                int deviceId = json.path("deviceid").asInt();
                int language = json.path("language").asInt();
                int volume = json.path("volume").asInt();
                int screensaver = json.path("screensaver").asInt();
                int verifymode = json.path("verifymode").asInt();
                int sleep = json.path("sleep").asInt();
                int userfpnum = json.path("userfpnum").asInt();
                int loghint = json.path("loghint").asInt();
                int reverifytime = json.path("reverifytime").asInt();

                System.out.println("✅ Respuesta GETDEVINFO recibida correctamente:");
                System.out.printf(
                        """
                        📊 Parámetros del terminal:
                        - deviceid: %d
                        - language: %d
                        - volume: %d
                        - screensaver: %d
                        - verifymode: %d
                        - sleep: %d
                        - userfpnum: %d
                        - loghint: %d
                        - reverifytime: %d
                        """,
                        deviceId, language, volume, screensaver, verifymode,
                        sleep, userfpnum, loghint, reverifytime
                );
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("⚠️ Falló GETDEVINFO. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al procesar respuesta de GETDEVINFO: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
