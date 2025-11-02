package com.icm.biometric_zone_gate_api.websocket.schedulers;

import com.icm.biometric_zone_gate_api.enums.DeviceStatus;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.websocket.DeviceSessionManager;
import com.icm.biometric_zone_gate_api.websocket.commands.SetTimeCommandSender;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.SetTimeDispatcher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PendingSetTimeScheduler {

    private final DeviceRepository deviceRepository;
    private final DeviceSessionManager sessionManager;
    private final SetTimeCommandSender setTimeCommandSender;
    private final SetTimeDispatcher dispatcher;

    @Scheduled(fixedDelay = 3000) // cada 3s
    @Transactional
    public void retryPendingSetTime() {
        if (dispatcher.allPendings().isEmpty()) return;

        var connected = deviceRepository.findByStatus(DeviceStatus.CONNECTED);
        if (connected.isEmpty()) return;

        var connectedBySn = connected.stream()
                .collect(Collectors.toMap(d -> d.getSn(), d -> d));

        dispatcher.allPendings().forEach((sn, pending) -> {
            if (!connectedBySn.containsKey(sn)) return;
            WebSocketSession session = sessionManager.getSessionBySn(sn);
            if (session == null || !session.isOpen()) return;
            if (!dispatcher.readyToRetry(pending)) return;

            try {
                // Envia con el cloudTime actualmente pendiente
                setTimeCommandSender.sendSetTimeCommand(session,
                        pending.getCloudTime()); // <-- añadiremos overload
                dispatcher.markSent(sn);
                System.out.printf("♻️ Reintentando SETTIME (sn=%s, intento=%d, cloudTime=%s)%n",
                        sn, pending.getAttempts(), pending.getCloudTime());
            } catch (Exception e) {
                System.err.printf("❌ Error reenviando SETTIME a %s: %s%n", sn, e.getMessage());
            }
        });
    }
}
