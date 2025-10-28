package com.icm.biometric_zone_gate_api.websocket.dispatchers;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class InitSystemDispatcher {

    @Data
    public static class PendingInit {
        private final String deviceSn;
        private final Instant createdAt = Instant.now();
    }

    // deviceSn -> FIFO queue
    private final Map<String, Queue<PendingInit>> queues = new HashMap<>();

    /** Registrar INIT pendiente */
    public synchronized void register(String deviceSn) {
        queues.computeIfAbsent(deviceSn, k -> new LinkedList<>())
                .add(new PendingInit(deviceSn));
        System.out.printf("📥 INIT SYS queued: (sn=%s)%n", deviceSn);
    }

    /** Consumir el ACK → cierra ciclo */
    public synchronized Optional<PendingInit> poll(String deviceSn) {
        Queue<PendingInit> q = queues.get(deviceSn);
        if (q == null || q.isEmpty()) return Optional.empty();
        return Optional.ofNullable(q.poll());
    }

    /** ¿Hay init pendientes para este dispositivo? */
    public synchronized boolean hasPending(String deviceSn) {
        Queue<PendingInit> q = queues.get(deviceSn);
        return q != null && !q.isEmpty();
    }
}
