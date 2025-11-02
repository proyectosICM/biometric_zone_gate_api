package com.icm.biometric_zone_gate_api.websocket.schedulers;

import com.icm.biometric_zone_gate_api.enums.DeviceStatus;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.websocket.DeviceSessionManager;
import com.icm.biometric_zone_gate_api.websocket.commands.CleanAdminCommandSender;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.CleanAdminDispatcher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PendingCleanAdminScheduler {

    private final DeviceRepository deviceRepository;
    private final DeviceSessionManager sessionManager;
    private final CleanAdminCommandSender cleanAdminCommandSender;
    private final CleanAdminDispatcher dispatcher;

    @Scheduled(fixedDelay = 3000) // cada 3s
    @Transactional
    public void retryPendingCleanAdmin() {
        if (dispatcher.allPendings().isEmpty()) return;

        var connected = deviceRepository.findByStatus(DeviceStatus.CONNECTED);
        if (connected.isEmpty()) return;

        var connectedBySn = connected.stream().collect(Collectors.toMap(d -> d.getSn(), d -> d));

        dispatcher.allPendings().forEach((sn, pending) -> {
            if (!connectedBySn.containsKey(sn)) return;

            WebSocketSession session = sessionManager.getSessionBySn(sn);
            if (session == null || !session.isOpen()) return;

            if (!dispatcher.readyToRetry(pending)) return;

            try {
                cleanAdminCommandSender.sendCleanAdminCommand(session);
                dispatcher.markSent(sn);
                System.out.printf("Reintentando CLEANADMIN (sn=%s, intento=%d)%n",
                        sn, pending.getAttempts());
            } catch (Exception e) {
                System.err.printf("Error reenviando CLEANADMIN a %s: %s%n", sn, e.getMessage());
            }
        });
    }
}
