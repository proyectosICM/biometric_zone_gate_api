package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.InitSystemDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * Maneja la respuesta del dispositivo al comando "initsys".
 */
@Component
@RequiredArgsConstructor
public class InitSystemResponseHandler {

    private final InitSystemDispatcher initSystemDispatcher;

    public void handleInitSystemResponse(JsonNode json, WebSocketSession session) {
        try {
            String ret = json.path("ret").asText("");
            if (!"initsys".equalsIgnoreCase(ret)) return;

            boolean result = json.path("result").asBoolean(false);
            String sn = (String) session.getAttributes().get("sn");

            if (sn == null) {
                System.err.println("No se encontró SN en sesión para ACK INIT SYSTEM");
                return;
            }

            var pending = initSystemDispatcher.poll(sn);
            if (pending.isEmpty()) {
                System.err.println("ACK INIT SYSTEM recibido sin pending en dispatcher");
                return;
            }

            if (result) {
                System.out.println("✅ Dispositivo confirmó INIT SYSTEM exitoso (usuarios y logs eliminados).");
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("⚠️ Falló INIT SYSTEM. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al procesar respuesta de initsys: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
