package com.icm.biometric_zone_gate_api.websocket.schedulers;

import com.icm.biometric_zone_gate_api.enums.DeviceStatus;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.websocket.DeviceSessionManager;
import com.icm.biometric_zone_gate_api.websocket.commands.SetDevInfoCommandSender;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.SetDevInfoDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PendingSetDevInfoScheduler {

    private final DeviceRepository deviceRepository;
    private final DeviceSessionManager sessionManager;
    private final SetDevInfoCommandSender sender;
    private final SetDevInfoDispatcher dispatcher;

    @Scheduled(fixedDelay = 5000)
    public void retryPendingSetDevInfo() {
        var devices = deviceRepository.findByStatus(DeviceStatus.CONNECTED);
        if (devices.isEmpty()) return;

        for (var dev : devices) {
            var sn = dev.getSn();
            if (!dispatcher.hasPending(sn)) continue;

            var session = sessionManager.getSessionBySn(sn);
            if (session == null || !session.isOpen()) continue;

            var payload = dispatcher.nextPayload(sn);
            if (payload == null) continue;

            try {
                sender.sendSetDevInfoCommand(session, payload);
                dispatcher.markSent(sn);
                System.out.printf("Reenviando SETDEVINFO a %s%n", sn);
            } catch (Exception e) {
                System.err.printf("Error reenviando SETDEVINFO a %s: %s%n", sn, e.getMessage());
            }
        }
    }
}
