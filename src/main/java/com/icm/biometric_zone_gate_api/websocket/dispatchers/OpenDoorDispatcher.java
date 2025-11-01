package com.icm.biometric_zone_gate_api.websocket.dispatchers;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class OpenDoorDispatcher {

    // Política de reintentos/tiempos
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration WINDOW = Duration.ofMinutes(1);      // ventana total
    private static final Duration RETRY_COOLDOWN = Duration.ofSeconds(5); // entre intentos

    @Data
    public static class PendingOpenDoor {
        private final String deviceSn;
        private final Integer doorNum; // null = abrir todas
        private final Instant createdAt = Instant.now();
        private Instant lastSentAt;
        private int attempts;

        public boolean isExpired() {
            return Instant.now().isAfter(createdAt.plus(WINDOW));
        }

        public boolean canRetryNow() {
            if (attempts == 0) return true;
            if (lastSentAt == null) return true;
            return Instant.now().isAfter(lastSentAt.plus(RETRY_COOLDOWN));
        }

        public boolean attemptsLeft() {
            return attempts < MAX_ATTEMPTS;
        }

        public void markSent() {
            attempts++;
            lastSentAt = Instant.now();
        }
    }

    // deviceSn -> cola FIFO de pendientes
    private final Map<String, Queue<PendingOpenDoor>> queues = new ConcurrentHashMap<>();

    /** Registrar un open door pendiente (se empuja a la cola del dispositivo) */
    public void register(String deviceSn, Integer doorNum) {
        queues.computeIfAbsent(deviceSn, k -> new ConcurrentLinkedQueue<>())
                .add(new PendingOpenDoor(deviceSn, doorNum));
        System.out.printf("📥 OpenDoor queued: sn=%s, door=%s%n", deviceSn, doorNum);
    }

    /** ACK: al recibir éxito, consumimos cabeza FIFO */
    public Optional<PendingOpenDoor> ack(String deviceSn) {
        Queue<PendingOpenDoor> q = queues.get(deviceSn);
        if (q == null) return Optional.empty();
        var item = q.poll();
        if (item != null) {
            System.out.printf("✅ OpenDoor ACK: sn=%s, door=%s%n", deviceSn, item.getDoorNum());
            return Optional.of(item);
        }
        return Optional.empty();
    }

    /** ¿Hay pendientes? */
    public boolean hasPending(String deviceSn) {
        Queue<PendingOpenDoor> q = queues.get(deviceSn);
        return q != null && !q.isEmpty();
    }

    /** Ver el siguiente candidato a reintentar (no lo quita de la cola) */
    public Optional<PendingOpenDoor> peek(String deviceSn) {
        Queue<PendingOpenDoor> q = queues.get(deviceSn);
        if (q == null) return Optional.empty();
        return Optional.ofNullable(q.peek());
    }

    /** Descartar cabeza (por expirado o sin intentos restantes) */
    public Optional<PendingOpenDoor> dropHead(String deviceSn, String reasonLog) {
        Queue<PendingOpenDoor> q = queues.get(deviceSn);
        if (q == null) return Optional.empty();
        var item = q.poll();
        if (item != null) {
            System.out.printf("🗑️ Drop OpenDoor: sn=%s, door=%s, reason=%s%n",
                    deviceSn, item.getDoorNum(), reasonLog);
            return Optional.of(item);
        }
        return Optional.empty();
    }
}
