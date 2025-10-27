package com.icm.biometric_zone_gate_api.websocket.dispatchers;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class SetUserNameDispatcher {

    @Data
    public static class PendingName {
        private final String deviceSn;
        private final int enrollId;
        private final String name;     // nombre que intentamos aplicar
        private final Instant sentAt = Instant.now();
    }

    // deviceSn -> cola FIFO
    private final Map<String, Queue<PendingName>> queues = new HashMap<>();

    /** Registrar envío pendiente */
    public synchronized void register(String deviceSn, int enrollId, String name) {
        queues.computeIfAbsent(deviceSn, k -> new LinkedList<>())
                .add(new PendingName(deviceSn, enrollId, name));
        System.out.printf("📥 NameDispatcher: pendiente (sn=%s, enrollId=%d, name='%s')%n",
                deviceSn, enrollId, name);
    }

    /** Consumir ACK → retorna el PendingName confirmado (FIFO) */
    public synchronized Optional<PendingName> ack(String deviceSn) {
        Queue<PendingName> q = queues.get(deviceSn);
        if (q == null || q.isEmpty()) return Optional.empty();
        var item = q.poll();
        if (item != null) {
            System.out.printf("📤 NameDispatcher ACK (sn=%s, enrollId=%d, name='%s')%n",
                    deviceSn, item.getEnrollId(), item.getName());
            return Optional.of(item);
        }
        return Optional.empty();
    }

    public synchronized boolean hasPending(String deviceSn) {
        Queue<PendingName> q = queues.get(deviceSn);
        return q != null && !q.isEmpty();
    }
}
