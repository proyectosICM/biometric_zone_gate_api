package com.icm.biometric_zone_gate_api.websocket.schedulers;

import com.icm.biometric_zone_gate_api.enums.DeviceStatus;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.websocket.DeviceSessionManager;
import com.icm.biometric_zone_gate_api.websocket.commands.CleanLogCommandSender;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.CleanLogDispatcher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
@RequiredArgsConstructor
public class PendingCleanLogScheduler {

    private final DeviceRepository deviceRepository;
    private final DeviceSessionManager sessionManager;
    private final CleanLogCommandSender cleanLogCommandSender;
    private final CleanLogDispatcher dispatcher;

    @Scheduled(fixedDelay = 5000) // cada 3s
    @Transactional
    public void retryPendingCleanLog() {
        if (dispatcher.allPendings().isEmpty()) return;

        // Filtramos por devices CONNECTED para ahorrar trabajo
        var connected = deviceRepository.findByStatus(DeviceStatus.CONNECTED);
        if (connected.isEmpty()) return;

        var connectedBySn = connected.stream().collect(java.util.stream.Collectors.toMap(
                d -> d.getSn(), d -> d
        ));

        dispatcher.allPendings().forEach((sn, pending) -> {
            var dev = connectedBySn.get(sn);
            if (dev == null) return;

            WebSocketSession session = sessionManager.getSessionBySn(sn);
            if (session == null || !session.isOpen()) return;

            if (!dispatcher.readyToRetry(pending)) return;

            try {
                cleanLogCommandSender.sendCleanLogCommand(session);
                dispatcher.markSent(sn);
                System.out.printf("Reintentando CLEANLOG (sn=%s, intento=%d)%n",
                        sn, pending.getAttempts());
            } catch (Exception e) {
                System.err.printf("Error reenviando CLEANLOG a %s: %s%n", sn, e.getMessage());
            }
        });
    }
}
