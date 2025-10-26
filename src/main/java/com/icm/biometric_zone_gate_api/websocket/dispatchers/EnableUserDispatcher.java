// EnableUserDispatcher.java
package com.icm.biometric_zone_gate_api.websocket.dispatchers;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class EnableUserDispatcher {

    @Data
    public static class PendingEnable {
        private final String deviceSn;
        private final int enrollId;
        private final boolean enabled; // lo que intentamos enviar
        private final Instant sentAt = Instant.now();
    }

    // deviceSn -> cola FIFO
    private final Map<String, Queue<PendingEnable>> queues = new HashMap<>();

    public synchronized void register(String deviceSn, int enrollId, boolean enabled) {
        queues.computeIfAbsent(deviceSn, k -> new LinkedList<>())
                .add(new PendingEnable(deviceSn, enrollId, enabled));
        System.out.printf("📥 EnableDispatcher: pendiente (sn=%s, enrollId=%d, enabled=%s)%n",
                deviceSn, enrollId, enabled);
    }

    public synchronized Optional<PendingEnable> ack(String deviceSn) {
        Queue<PendingEnable> q = queues.get(deviceSn);
        if (q == null || q.isEmpty()) return Optional.empty();
        var item = q.poll();
        if (item != null) {
            System.out.printf("📤 EnableDispatcher ACK (sn=%s, enrollId=%d, enabled=%s)%n",
                    deviceSn, item.getEnrollId(), item.isEnabled());
            return Optional.of(item);
        }
        return Optional.empty();
    }

    public synchronized boolean hasPending(String deviceSn) {
        Queue<PendingEnable> q = queues.get(deviceSn);
        return q != null && !q.isEmpty();
    }
}
