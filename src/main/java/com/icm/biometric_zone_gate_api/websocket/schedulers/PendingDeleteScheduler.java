package com.icm.biometric_zone_gate_api.websocket.schedulers;

import com.icm.biometric_zone_gate_api.enums.DeviceStatus;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.repositories.DeviceUserAccessRepository;
import com.icm.biometric_zone_gate_api.websocket.DeviceSessionManager;
import com.icm.biometric_zone_gate_api.websocket.commands.DeleteUserCommandSender;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PendingDeleteScheduler {

    private final DeviceRepository deviceRepository;
    private final DeviceUserAccessRepository deviceUserAccessRepository;
    private final DeviceSessionManager sessionManager;
    private final DeleteUserCommandSender deleteUserCommandSender;

    @Scheduled(fixedDelay = 5000) // cada 5s
    public void retryPendingDeletes() {
        var connectedDevices = deviceRepository.findByStatus(DeviceStatus.CONNECTED);
        if (connectedDevices.isEmpty()) return;

        for (var device : connectedDevices) {

            var session = sessionManager.getSessionBySn(device.getSn());
            if (session == null || !session.isOpen()) continue;

            var pending = deviceUserAccessRepository.findPendingDeleteWithUserAndCredentials(device.getId());
            if (pending.isEmpty()) continue;

            for (var access : pending) {
                var user = access.getUser();
                if (user == null) continue;

                int enrollId = access.getEnrollId();
                if (enrollId <= 0) continue;

                // Enviar un delete por cada credential
                user.getCredentials().forEach(cred -> {
                    deleteUserCommandSender.sendDeleteUserCommand(
                            session,
                            enrollId,
                            cred.getBackupNum()
                    );
                    System.out.printf("♻️ Reintentando DELETE (enrollId=%d, backup=%d) dev=%s%n",
                            enrollId, cred.getBackupNum(), device.getSn());
                });
            }
        }
    }
}
