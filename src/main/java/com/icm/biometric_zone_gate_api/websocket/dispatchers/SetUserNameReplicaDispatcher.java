package com.icm.biometric_zone_gate_api.websocket.dispatchers;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class SetUserNameReplicaDispatcher {

    @Data
    public static class PendingReplica {
        private final String deviceSn;
        private final int enrollId;
        private final String name;
        private final Instant createdAt = Instant.now();
    }

    // deviceSn -> cola FIFO
    private final Map<String, Queue<PendingReplica>> queues = new HashMap<>();

    /** Encola una réplica de nombre para un dispositivo */
    public synchronized void register(String deviceSn, int enrollId, String name) {
        queues
                .computeIfAbsent(deviceSn, k -> new LinkedList<>())
                .add(new PendingReplica(deviceSn, enrollId, name));
        System.out.printf("📥 NameReplica queued: (sn=%s, enrollId=%d, name='%s')%n",
                deviceSn, enrollId, name);
    }

    /** Saca el próximo pendiente para ese SN (FIFO) */
    public synchronized Optional<PendingReplica> poll(String deviceSn) {
        Queue<PendingReplica> q = queues.get(deviceSn);
        if (q == null || q.isEmpty()) return Optional.empty();
        return Optional.ofNullable(q.poll());
    }

    /** ¿Hay réplicas pendientes para ese SN? */
    public synchronized boolean hasPending(String deviceSn) {
        Queue<PendingReplica> q = queues.get(deviceSn);
        return q != null && !q.isEmpty();
    }
}
