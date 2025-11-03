package com.icm.biometric_zone_gate_api.websocket.dispatchers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SetDevInfoDispatcher {

    @Data
    public static class Pending {
        private final String sn;
        private ObjectNode payload;   // JSON a enviar (parcial u opciones completas)
        private boolean sent;         // true cuando ya se envió por WS
        private Instant createdAt = Instant.now();
        private Instant lastSentAt;
        private int attempts;
    }

    // 1 por dispositivo (solo nos interesa el último “estado deseado”)
    private final Map<String, Pending> pendings = new ConcurrentHashMap<>();

    /** Crea o actualiza el payload pendiente para un SN. */
    public synchronized void registerOrUpdate(String sn, ObjectNode payload) {
        var p = pendings.computeIfAbsent(sn, k -> new Pending(sn));
        p.setPayload(payload);
        p.setSent(false);
        p.setCreatedAt(Instant.now());
        p.setLastSentAt(null);
        p.setAttempts(0);
        System.out.printf("📥 SETDEVINFO queued/updated (sn=%s)\n", sn);
    }

    public synchronized void markSent(String sn) {
        var p = pendings.get(sn);
        if (p != null) {
            p.setSent(true);
            p.setLastSentAt(Instant.now());
            p.setAttempts(p.getAttempts() + 1);
        }
    }

    /** Llamar cuando llega el ACK (result=true). */
    public synchronized void ack(String sn) {
        if (pendings.remove(sn) != null) {
            System.out.printf("✅ SETDEVINFO ACK (sn=%s) → limpiado\n", sn);
        }
    }

    public synchronized Pending get(String sn) { return pendings.get(sn); }

    public synchronized boolean hasPending(String sn) {
        return pendings.containsKey(sn);
    }

    /** Si existe y no está enviado o quieres reenviar, devuelve el payload. */
    public synchronized ObjectNode nextPayload(String sn) {
        var p = pendings.get(sn);
        return (p == null) ? null : p.getPayload();
    }

    /** Limpia sin ACK (si necesitas cancelar). */
    public synchronized void clear(String sn) { pendings.remove(sn); }
}
