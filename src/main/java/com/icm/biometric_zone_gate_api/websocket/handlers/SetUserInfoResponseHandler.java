package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.repositories.DeviceUserAccessRepository;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.SetUserInfoDispatcher;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.SetUserInfoReplicaDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
@RequiredArgsConstructor
public class SetUserInfoResponseHandler {

    private final SetUserInfoDispatcher dispatcher;              // sincronización normal
    private final SetUserInfoReplicaDispatcher replicaDispatcher; // réplicas desde otros dispositivos
    private final DeviceUserAccessRepository deviceUserAccessRepository;

    public void handleSetUserInfoResponse(JsonNode json, WebSocketSession session) {
        try {
            String ret = json.path("ret").asText("");
            boolean result = json.path("result").asBoolean(false);

            if (!"setuserinfo".equalsIgnoreCase(ret)) {
                return;
            }

            String sn = (String) session.getAttributes().get("sn");
            if (sn == null) {
                System.err.println("❌ No SN en sesión para ACK setuserinfo");
                return;
            }

            int enrollId = json.path("enrollid").asInt(-1);
            int backupNum = json.path("backupnum").asInt(-1);

            if (!result) {
                int reason = json.path("reason").asInt(-1);
                System.out.printf("❌ Dispositivo rechazó SETUSERINFO (sn=%s, enroll=%d, backup=%d) reason=%d%n",
                        sn, enrollId, backupNum, reason);
                return;
            }

            // ✅ 1) ¿Este ACK corresponde a una réplica?
            var replicaPending = replicaDispatcher.poll(sn);
            if (replicaPending.isPresent()) {
                var r = replicaPending.get();
                System.out.printf("✅ ACK Replica confirmada (sn=%s, enroll=%d, backup=%d) [NO se marca synced]\n",
                        sn, r.getEnrollId(), r.getBackupNum());
                return; // NO seguimos al flujo normal
            }

            // ✅ 2) Si no era réplica → flujo normal
            var normalPending = dispatcher.ack(sn);
            if (normalPending.isEmpty()) {
                System.out.printf("⚠️ ACK recibido sin pending normal ni réplica (sn=%s, enrollId=%d)\n", sn, enrollId);
                return;
            }

            boolean stillPending = dispatcher.hasPending(sn, enrollId);
            if (!stillPending) {
                // ya no quedan más credenciales → marcamos synced=true
                deviceUserAccessRepository
                        .findByDeviceSnAndEnrollIdAndPendingDeleteFalse(sn, enrollId)
                        .ifPresent(access -> {
                            access.setSynced(true);
                            deviceUserAccessRepository.save(access);
                            System.out.printf("✅ setuserinfo completado → synced=true (sn=%s, enrollId=%d)\n",
                                    sn, enrollId);
                        });
            }

        } catch (Exception e) {
            System.err.println("Error procesando ACK setuserinfo: " + e.getMessage());
        }
    }
}
