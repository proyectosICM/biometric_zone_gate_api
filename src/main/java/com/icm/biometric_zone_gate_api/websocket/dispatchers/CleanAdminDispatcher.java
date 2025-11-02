package com.icm.biometric_zone_gate_api.websocket.dispatchers;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CleanAdminDispatcher {

    @Data
    public static class Pending {
        private final String deviceSn;
        private final Instant createdAt = Instant.now();
        private Instant lastSentAt;   // null al inicio
        private int attempts;         // 0 al inicio
    }

    // sn -> pending (uno por dispositivo)
    private final Map<String, Pending> pendings = new ConcurrentHashMap<>();

    /** Registra (o mantiene) un CLEANADMIN pendiente para el SN. Idempotente. */
    public void register(String sn) {
        pendings.computeIfAbsent(sn, Pending::new);
        System.out.printf("📥 CLEANADMIN pendiente registrado (sn=%s)%n", sn);
    }

    /** Marca intento de envío (actualiza attempts + timestamp). */
    public void markSent(String sn) {
        var p = pendings.get(sn);
        if (p != null) {
            p.attempts++;
            p.lastSentAt = Instant.now();
        }
    }

    /** Confirma (ACK) y limpia. */
    public void confirm(String sn) {
        if (pendings.remove(sn) != null) {
            System.out.printf("✅ CLEANADMIN confirmado (sn=%s) — se limpia del dispatcher%n", sn);
        }
    }

    /** ¿Hay pendiente para este SN? */
    public boolean hasPending(String sn) {
        return pendings.containsKey(sn);
    }

    /** Lectura para el scheduler. */
    public Map<String, Pending> allPendings() {
        return pendings;
    }

    /** Backoff exponencial simple: 2s, 4s, 8s, máx 30s. */
    public boolean readyToRetry(Pending p) {
        if (p.lastSentAt == null) return true;
        long sec = Math.min(30, (long) Math.pow(2, Math.max(1, p.attempts)));
        return Duration.between(p.lastSentAt, Instant.now()).getSeconds() >= sec;
    }
}
