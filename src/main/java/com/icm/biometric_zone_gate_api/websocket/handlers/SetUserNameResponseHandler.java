package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.repositories.DeviceUserAccessRepository;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.SetUserNameDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * Maneja la respuesta del dispositivo al comando "setusername".
 */
@Component
@RequiredArgsConstructor
public class SetUserNameResponseHandler {

    private final SetUserNameDispatcher dispatcher;
    private final DeviceUserAccessRepository deviceUserAccessRepository;

    public void handleSetUserNameResponse(JsonNode json, WebSocketSession session) {
        try {
            String ret = json.path("ret").asText("");
            boolean result = json.path("result").asBoolean(false);
            if (!"setusername".equalsIgnoreCase(ret)) return;

            String sn = (String) session.getAttributes().get("sn");
            if (sn == null) {
                System.err.println("⚠ No SN en sesión para ACK setusername");
                return;
            }

            var opt = dispatcher.ack(sn);
            if (opt.isEmpty()) {
                System.err.println("⚠ ACK setusername sin pending en dispatcher");
                return;
            }

            var pending = opt.get();
            int enrollId = pending.getEnrollId();

            if (result) {
                System.out.println("Dispositivo confirmó SET USERNAME exitoso.");
                deviceUserAccessRepository
                        .findByDeviceSnAndEnrollIdAndPendingDeleteFalseFetchUser(sn, enrollId)
                        .ifPresent(access -> {
                            access.setPendingNameSync(false); // ✅ confirmado
                            deviceUserAccessRepository.save(access);
                            System.out.printf("✅ Nombre aplicado y confirmado (sn=%s, enrollId=%d)%n",
                                    sn, enrollId);
                        });
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("Falló SET USERNAME. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("Error al procesar respuesta de setusername: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
