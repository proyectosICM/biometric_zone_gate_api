// CleanUserDispatcher.java
package com.icm.biometric_zone_gate_api.websocket.dispatchers;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class CleanUserDispatcher {

    @Data
    public static class PendingClean {
        private final String deviceSn;
        private final Instant createdAt = Instant.now();
    }

    private final Map<String, Queue<PendingClean>> queues = new HashMap<>();

    public synchronized void register(String deviceSn) {
        queues.computeIfAbsent(deviceSn, k -> new LinkedList<>())
                .add(new PendingClean(deviceSn));
        System.out.printf("📥 CleanUser pending queued (sn=%s)%n", deviceSn);
    }

    public synchronized Optional<PendingClean> poll(String sn) {
        var queue = queues.get(sn);
        if (queue == null || queue.isEmpty()) return Optional.empty();
        return Optional.ofNullable(queue.poll());
    }

    public synchronized boolean hasPending(String sn) {
        var queue = queues.get(sn);
        return queue != null && !queue.isEmpty();
    }
}
