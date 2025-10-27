package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.repositories.DeviceUserAccessRepository;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.CleanUserDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * Maneja la respuesta del dispositivo al comando "cleanuser".
 */
@Component
@RequiredArgsConstructor
public class CleanUserResponseHandler {

    private final CleanUserDispatcher dispatcher;
    private final DeviceRepository deviceRepository;
    private final DeviceUserAccessRepository deviceUserAccessRepository;

    public void handleCleanUserResponse(JsonNode json, WebSocketSession session) {
        try {
            boolean result = json.path("result").asBoolean(false);
            String sn = (String) session.getAttributes().get("sn");

            if (!"cleanuser".equalsIgnoreCase(json.path("ret").asText(""))) return;
            if (sn == null) return;

            var pending = dispatcher.poll(sn);
            if (pending.isEmpty()) {
                System.err.println("⚠ ACK CLEANUSER recibido sin pending");
                return;
            }

            if (result) {
                System.out.println("Dispositivo confirmó CLEAN USER exitoso.");

                deviceRepository.findBySn(sn).ifPresent(device -> {
                    deviceUserAccessRepository.deleteByDeviceId(device.getId());
                    device.setPendingClean(false);
                    deviceRepository.save(device);
                    System.out.printf("🧹 AccessLinks eliminados para device=%s%n", sn);
                });
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("Falló CLEAN USER. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al procesar respuesta de cleanuser: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
