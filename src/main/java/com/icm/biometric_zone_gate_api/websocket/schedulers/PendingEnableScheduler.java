// PendingEnableScheduler.java
package com.icm.biometric_zone_gate_api.websocket.schedulers;

import com.icm.biometric_zone_gate_api.enums.DeviceStatus;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.repositories.DeviceUserAccessRepository;
import com.icm.biometric_zone_gate_api.websocket.DeviceSessionManager;
import com.icm.biometric_zone_gate_api.websocket.commands.EnableUserCommandSender;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.EnableUserDispatcher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PendingEnableScheduler {

    private final DeviceRepository deviceRepository;
    private final DeviceUserAccessRepository deviceUserAccessRepository;
    private final DeviceSessionManager sessionManager;
    private final EnableUserCommandSender enableUserCommandSender;
    private final EnableUserDispatcher dispatcher;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void retryPendingEnableFlags() {

        var connected = deviceRepository.findByStatus(DeviceStatus.CONNECTED);
        if (connected.isEmpty()) return;

        for (var device : connected) {
            if (device.getSn() == null || device.getSn().isBlank()) continue;

            var session = sessionManager.getSessionBySn(device.getSn());
            if (session == null || !session.isOpen()) continue;

            var pendings = deviceUserAccessRepository.findPendingStateSyncWithUser(device.getId());
            if (pendings.isEmpty()) continue;

            for (var access : pendings) {
                if (Boolean.TRUE.equals(access.isPendingDelete())) continue;

                var user = access.getUser();

                if (user == null || user.getId() == null) {
                    System.out.printf("Access sin usuario válido → removiendo pendingStateSync id=%d%n", access.getId());
                    access.setPendingStateSync(false);
                    deviceUserAccessRepository.save(access);
                    continue;
                }

                int enrollId = access.getEnrollId();
                if (enrollId <= 0) continue;

                boolean enabled = Boolean.TRUE.equals(user.getEnabled());

                try {
                    enableUserCommandSender.sendEnableUserCommand(session, enrollId, enabled);
                    dispatcher.register(device.getSn(), enrollId, enabled);
                    System.out.printf("♻️ Reintentando ENABLEUSER (sn=%s, enrollId=%d, enabled=%s)%n",
                            device.getSn(), enrollId, enabled);
                } catch (Exception e) {
                    System.err.printf("❌ Error enviando ENABLEUSER (sn=%s, enrollId=%d): %s%n",
                            device.getSn(), enrollId, e.getMessage());
                }
            }
        }
    }
}
