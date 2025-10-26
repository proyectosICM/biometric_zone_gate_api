package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.repositories.DeviceUserAccessRepository;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.EnableUserDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * Maneja la respuesta del dispositivo al comando "enableuser".
 * Este mismo comando sirve para habilitar y deshabilitar usuarios.
 */
@Component
@RequiredArgsConstructor
public class EnableUserResponseHandler {

    private final EnableUserDispatcher dispatcher;
    private final DeviceUserAccessRepository deviceUserAccessRepository;

    public void handleEnableUserResponse(JsonNode json, WebSocketSession session) {
        try {
            String ret = json.path("ret").asText("");
            boolean result = json.path("result").asBoolean(false);
            if (!"enableuser".equalsIgnoreCase(ret)) return;

            String sn = (String) session.getAttributes().get("sn");
            if (sn == null) {
                System.err.println("⚠ No SN en sesión para ACK enableuser");
                return;
            }

            var opt = dispatcher.ack(sn);
            if (opt.isEmpty()) {
                System.err.println("⚠ ACK enableuser sin pending en dispatcher");
                return;
            }

            var pending = opt.get();
            int enrollId = pending.getEnrollId();

            if (result) {
                System.out.println("Dispositivo confirmó ENABLE/DISABLE USER exitoso.");
                deviceUserAccessRepository
                        .findByDeviceSnAndEnrollIdAndPendingDeleteFalseFetchUser(sn, enrollId)
                        .ifPresent(access -> {
                            access.setPendingStateSync(false); // ✅ confirmada
                            deviceUserAccessRepository.save(access);
                            System.out.printf("✅ ENABLE/DISABLE aplicado y confirmado (sn=%s, enrollId=%d)%n",
                                    sn, enrollId);
                        });
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("Falló ENABLE/DISABLE USER. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("Error al procesar respuesta de enableuser: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
