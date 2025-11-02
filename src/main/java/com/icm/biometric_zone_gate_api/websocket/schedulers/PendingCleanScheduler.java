package com.icm.biometric_zone_gate_api.websocket.schedulers;

import com.icm.biometric_zone_gate_api.enums.DeviceStatus;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.websocket.DeviceSessionManager;
import com.icm.biometric_zone_gate_api.websocket.commands.CleanUserCommandSender;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.CleanUserDispatcher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PendingCleanScheduler {

    private final DeviceRepository deviceRepository;
    private final DeviceSessionManager sessionManager;
    private final CleanUserCommandSender cleanUserCommandSender;
    private final CleanUserDispatcher dispatcher;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void retryPendingClean() {
        var devices = deviceRepository.findByStatus(DeviceStatus.CONNECTED);
        if (devices.isEmpty()) return;

        for (var device : devices) {
            if (!device.isPendingClean()) continue;

            var session = sessionManager.getSessionBySn(device.getSn());
            if (session == null || !session.isOpen()) continue;

            try {
                cleanUserCommandSender.sendCleanUserCommand(session);
                dispatcher.register(device.getSn());
                System.out.printf("Reenviando CLEANUSER a %s%n", device.getSn());
            } catch (Exception e) {
                System.err.printf("Error reenviando CLEANUSER a %s: %s%n",
                        device.getSn(), e.getMessage());
            }
        }
    }
}
