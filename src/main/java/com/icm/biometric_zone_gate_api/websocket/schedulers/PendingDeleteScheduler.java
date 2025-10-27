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

                if (user == null) {
                    System.out.printf("Usuario de accessId=%d ya no existe. Eliminando acceso huérfano...%n", access.getId());
                    deviceUserAccessRepository.delete(access);
                    continue;
                }

                // si nunca se sincronizó → no tiene sentido mandar DELETE al dispositivo
                if (!Boolean.TRUE.equals(access.isSynced())) {
                    System.out.printf("ℹ️ Access (enrollId=%d) nunca sincronizado → se elimina solo en BD%n",
                            access.getEnrollId());
                    deviceUserAccessRepository.delete(access);
                    continue;
                }

                int enrollId = access.getEnrollId();
                if (enrollId <= 0) continue;

                var creds = user.getCredentials();
                if (creds == null || creds.isEmpty()) {
                    System.out.printf("Access enrollId=%d sin credenciales. Eliminando acceso...%n", enrollId);
                    deviceUserAccessRepository.delete(access);
                    continue;
                }

                deleteUserCommandSender.sendDeleteUserCommand(
                        session,
                        enrollId,
                        13 // <-- backupnum = 13 = delete ALL (fp + card + password)
                );
                // Enviar un delete por cada credential
                /*
                creds.forEach(cred -> {
                    deleteUserCommandSender.sendDeleteUserCommand(
                            session,
                            enrollId,
                            cred.getBackupNum()
                    );
                    System.out.printf("Reintentando DELETE (enrollId=%d, backup=%d) dev=%s%n",
                            enrollId, cred.getBackupNum(), device.getSn());
                });

                 */
            }
        }
    }
}
