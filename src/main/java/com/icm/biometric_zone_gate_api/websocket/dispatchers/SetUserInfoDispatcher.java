package com.icm.biometric_zone_gate_api.websocket.dispatchers;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class SetUserInfoDispatcher {

    @Data
    public static class PendingSet {
        private final String deviceSn;
        private final int enrollId;
        private final int backupNum;
        private final Instant sentAt = Instant.now();
    }

    // deviceSn -> (enrollId -> queue de pending sets)
    //private final Map<String, Map<Integer, Queue<PendingSet>>> queues = new HashMap<>();
    private final Map<String, LinkedHashMap<Integer, Deque<PendingSet>>> queues = new LinkedHashMap<>();


    /**
     * Registrar un envío pendiente
     */

    private boolean containsPending(Deque<PendingSet> q, int backupNum) {
        for (PendingSet p : q) {
            if (p.getBackupNum() == backupNum) return true;
        }
        return false;
    }


    public synchronized void register(String deviceSn, int enrollId, int backupNum) {
        var byEnroll = queues.computeIfAbsent(deviceSn, k -> new LinkedHashMap<>());
        var q = byEnroll.computeIfAbsent(enrollId, k -> new ArrayDeque<>());

        // único cambio: evitar duplicar (enrollId, backupNum)
        if (!containsPending(q, backupNum)) {
            q.addLast(new PendingSet(deviceSn, enrollId, backupNum));
            System.out.printf("📥 Dispatcher: pendiente = (sn=%s, enrollId=%d, backupNum=%d)%n",
                    deviceSn, enrollId, backupNum);
        } else {
            System.out.printf("↩️ Dispatcher: ya pendiente (sn=%s, enrollId=%d, backupNum=%d)%n",
                    deviceSn, enrollId, backupNum);
        }
    }
    //.computeIfAbsent(deviceSn, k -> new HashMap<>())
    // .computeIfAbsent(enrollId, k -> new LinkedList<>())
    /*
    public synchronized void register(String deviceSn, int enrollId, int backupNum) {
        queues
                .computeIfAbsent(deviceSn, k -> new LinkedHashMap<>())
                .computeIfAbsent(enrollId, k -> new ArrayDeque<>())
                .add(new PendingSet(deviceSn, enrollId, backupNum));

        System.out.printf("📥 Dispatcher: pendiente = (sn=%s, enrollId=%d, backupNum=%d)%n",
                deviceSn, enrollId, backupNum);
    }

     */

    /**
     * Consumir ACK: retorna el PendingSet confirmado
     */
    public synchronized Optional<PendingSet> ack(String deviceSn, int enrollId, int backupNum) {
        //Map<Integer, Queue<PendingSet>> deviceQueues = queues.get(deviceSn);
        LinkedHashMap<Integer, Deque<PendingSet>> deviceQueues = queues.get(deviceSn);
        if (deviceQueues == null || deviceQueues.isEmpty()) return Optional.empty();

        // Hay que tomar el primer enrollId en la cola
        //var enrollEntry = deviceQueues.entrySet().iterator().next();
        //int enrollId = enrollEntry.getKey();
        //Queue<PendingSet> q = enrollEntry.getValue();
        Deque<PendingSet> q = deviceQueues.get(enrollId);
        if (q == null || q.isEmpty()) return Optional.empty();

        PendingSet head = q.peekFirst();
        if (head == null) return Optional.empty();

        // (Opcional) validar backupNum al frente de la cola
        if (head.getBackupNum() != backupNum) {
            // Si no coinciden, no consumimos: ACK fuera de orden vs lo que enviamos primero
            System.out.printf("⚠️ ACK con backupNum inesperado (sn=%s, enrollId=%d, esperado=%d, recibido=%d)%n",
                    deviceSn, enrollId, head.getBackupNum(), backupNum);
            return Optional.empty();
        }

        PendingSet item = q.pollFirst();
        if (q.isEmpty()) {
            deviceQueues.remove(enrollId);
        }

        System.out.printf("📤 Dispatcher ACK (sn=%s, enrollId=%d, backupNum=%d)%n",
                deviceSn, item.getEnrollId(), item.getBackupNum());

        return Optional.of(item);
    }

    /**
     * ¿Quedan pendientes para este enrollId específicamente?
     */
    public synchronized boolean hasPending(String deviceSn, int enrollId) {
        //Map<Integer, Queue<PendingSet>> deviceQueues = queues.get(deviceSn);
        var deviceQueues = queues.get(deviceSn);
        if (deviceQueues == null) return false;

        //Queue<PendingSet> q = deviceQueues.get(enrollId);
        Deque<PendingSet> q = deviceQueues.get(enrollId);
        return q != null && !q.isEmpty();
    }
}
