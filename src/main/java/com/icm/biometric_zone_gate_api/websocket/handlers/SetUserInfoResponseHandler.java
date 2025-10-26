package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.repositories.DeviceUserAccessRepository;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.SetUserInfoDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * Maneja la respuesta del dispositivo al comando "setuserinfo".
 */
@Component
@RequiredArgsConstructor
public class SetUserInfoResponseHandler {
    private final SetUserInfoDispatcher dispatcher;
    private final DeviceUserAccessRepository deviceUserAccessRepository;

    public void handleSetUserInfoResponse(JsonNode json, WebSocketSession session) {
        try {
            String ret = json.path("ret").asText("");
            boolean result = json.path("result").asBoolean(false);

            if (!"setuserinfo".equalsIgnoreCase(ret)) {
                return;
            }

            // Obtener SN desde la sesión
            String sn = (String) session.getAttributes().get("sn");
            if (sn == null) {
                System.err.println("No se puede asociar ACK de setuserinfo: SN no encontrado en sesión");
                return;
            }

            // Consumir el pending del dispatcher (primero de la cola FIFO)
            var opt = dispatcher.ack(sn);

            if (opt.isEmpty()) {
                System.err.println("ACK recibido pero no hay pending en dispatcher");
                return;
            }

            var pending = opt.get();
            int enrollId = pending.getEnrollId();
            int backupNum = pending.getBackupNum();

            if (result) {
                System.out.printf("✅ CONFIRMADO: setuserinfo (sn=%s, enroll=%d, backup=%d)%n (descarga)",
                        sn, enrollId, backupNum);
                // AHORA: si la cola terminó vacía → recién marcamos synced=true
                boolean stillPending = dispatcher.hasPending(sn, enrollId);

                if (!stillPending) {
                    deviceUserAccessRepository
                            .findByDeviceSnAndEnrollIdAndPendingDeleteFalse(sn, enrollId)
                            .ifPresent(access -> {
                                access.setSynced(true);
                                deviceUserAccessRepository.save(access);
                                System.out.printf("✅ Todos los backup confirmados → synced=true para enrollId=%d en %s%n",
                                        enrollId, sn);
                            });
                }
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("Dispositivo rechazó SETUSERINFO. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("Error al procesar respuesta de setuserinfo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
