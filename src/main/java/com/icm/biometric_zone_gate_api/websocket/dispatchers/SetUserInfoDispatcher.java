package com.icm.biometric_zone_gate_api.websocket.dispatchers;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class SetUserInfoDispatcher {

    @Data
    public static class PendingSet {
        private final String deviceSn;
        private final int enrollId;
        private final int backupNum;
        private final Instant sentAt = Instant.now();
    }

    // deviceSn -> (enrollId -> queue de pending sets)
    private final Map<String, Map<Integer, Queue<PendingSet>>> queues = new HashMap<>();

    /** Registrar un envío pendiente */
    public synchronized void register(String deviceSn, int enrollId, int backupNum) {
        queues.computeIfAbsent(deviceSn, k -> new HashMap<>())
                .computeIfAbsent(enrollId, k -> new LinkedList<>())
                .add(new PendingSet(deviceSn, enrollId, backupNum));

        System.out.printf("📥 Dispatcher: pendiente = (sn=%s, enrollId=%d, backupNum=%d)%n",
                deviceSn, enrollId, backupNum);
    }

    /** Consumir ACK: retorna el PendingSet confirmado */
    public synchronized Optional<PendingSet> ack(String deviceSn) {
        Map<Integer, Queue<PendingSet>> deviceQueues = queues.get(deviceSn);
        if (deviceQueues == null || deviceQueues.isEmpty()) return Optional.empty();

        // Hay que tomar el primer enrollId en la cola
        var enrollEntry = deviceQueues.entrySet().iterator().next();
        int enrollId = enrollEntry.getKey();
        Queue<PendingSet> q = enrollEntry.getValue();

        if (q == null || q.isEmpty()) return Optional.empty();

        PendingSet item = q.poll();
        if (q.isEmpty()) {
            // Si ya no quedan credenciales para ESTE enrollId → eliminar su cola
            deviceQueues.remove(enrollId);
        }

        System.out.printf("📤 Dispatcher ACK (sn=%s, enrollId=%d, backupNum=%d)%n",
                deviceSn, item.getEnrollId(), item.getBackupNum());

        return Optional.of(item);
    }

    /** ¿Quedan pendientes para este enrollId específicamente? */
    public synchronized boolean hasPending(String deviceSn, int enrollId) {
        Map<Integer, Queue<PendingSet>> deviceQueues = queues.get(deviceSn);
        if (deviceQueues == null) return false;

        Queue<PendingSet> q = deviceQueues.get(enrollId);
        return q != null && !q.isEmpty();
    }
}
