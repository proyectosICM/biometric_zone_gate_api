package com.icm.biometric_zone_gate_api.websocket.dispatchers;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class SetUserInfoReplicaDispatcher {

    @Data
    public static class PendingReplica {
        private final String deviceSn;
        private final int enrollId;
        private final int backupNum;
        private final Instant createdAt = Instant.now();
    }

    // deviceSn -> cola FIFO de réplicas
    private final Map<String, Queue<PendingReplica>> queues = new HashMap<>();

    /** Registrar una réplica pendiente */
    public synchronized void register(String deviceSn, int enrollId, int backupNum) {
        queues
                .computeIfAbsent(deviceSn, k -> new LinkedList<>())
                .add(new PendingReplica(deviceSn, enrollId, backupNum));

        System.out.printf("📥 Replica queued: (sn=%s, enrollId=%d, backupNum=%d)%n",
                deviceSn, enrollId, backupNum);
    }

    /** Sacar el siguiente pendiente FIFO */
    public synchronized Optional<PendingReplica> poll(String sn, int enrollId, int backupNum) {
        Queue<PendingReplica> q = queues.get(sn);
        if (q == null || q.isEmpty()) return Optional.empty();

        PendingReplica head = q.peek();
        if (head == null) return Optional.empty();

        if (head.getEnrollId() != enrollId || head.getBackupNum() != backupNum) {
            return Optional.empty(); // no corresponde todavía
        }

        PendingReplica taken = q.poll();
        if (q.isEmpty()) queues.remove(sn);
        return Optional.ofNullable(taken);
    }

    /** Verificar si hay réplicas en espera */
    public synchronized boolean hasPending(String sn) {
        Queue<PendingReplica> q = queues.get(sn);
        return q != null && !q.isEmpty();
    }
}
