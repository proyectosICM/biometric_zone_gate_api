package com.icm.biometric_zone_gate_api.websocket.dispatchers;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SetTimeDispatcher {

    @Data
    public static class Pending {
        private final String deviceSn;
        /** Cloud time solicitado (yyyy-MM-dd HH:mm:ss) */
        private volatile String cloudTime;
        private Instant createdAt = Instant.now();
        private volatile Instant lastSentAt;  // null al inicio
        private volatile int attempts;        // 0 al inicio
    }

    // sn -> pending (uno por dispositivo)
    private final Map<String, Pending> pendings = new ConcurrentHashMap<>();

    /** Registrar/actualizar un SETTIME pendiente (idempotente; el último cloudTime gana). */
    public void registerOrUpdate(String sn, String cloudTime) {
        pendings.compute(sn, (k, old) -> {
            if (old == null) {
                Pending p = new Pending(sn);
                p.setCloudTime(cloudTime);
                return p;
            }
            old.setCloudTime(cloudTime);
            // no reseteamos attempts/lastSentAt; seguimos el backoff actual
            return old;
        });
        System.out.printf("📥 SETTIME pendiente (sn=%s, cloudTime=%s)%n", sn, cloudTime);
    }

    /** Lectura para el scheduler. */
    public Map<String, Pending> allPendings() {
        return pendings;
    }

    /** Confirma (ACK) y limpia. */
    public void confirm(String sn) {
        if (pendings.remove(sn) != null) {
            System.out.printf("✅ SETTIME confirmado y retirado del dispatcher (sn=%s)%n", sn);
        }
    }

    /** Marca intento de envío. */
    public void markSent(String sn) {
        var p = pendings.get(sn);
        if (p != null) {
            p.attempts++;
            p.lastSentAt = Instant.now();
        }
    }

    /** ¿Hay pendiente? */
    public boolean hasPending(String sn) {
        return pendings.containsKey(sn);
    }

    /** Backoff exponencial simple: 2s, 4s, 8s… máx 30s. */
    public boolean readyToRetry(Pending p) {
        if (p.lastSentAt == null) return true;
        long sec = Math.min(30, (long) Math.pow(2, Math.max(1, p.attempts)));
        return Duration.between(p.lastSentAt, Instant.now()).getSeconds() >= sec;
    }
}
