package com.icm.biometric_zone_gate_api.websocket.schedulers;

import com.icm.biometric_zone_gate_api.enums.DeviceStatus;
import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.models.UserModel;
import com.icm.biometric_zone_gate_api.models.UserCredentialModel;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.repositories.UserRepository;
import com.icm.biometric_zone_gate_api.repositories.UserCredentialRepository;
import com.icm.biometric_zone_gate_api.websocket.DeviceSessionManager;
import com.icm.biometric_zone_gate_api.websocket.commands.SetUserInfoCommandSender;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.SetUserInfoReplicaDispatcher;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.SetUserInfoReplicaDispatcher.PendingReplica;
import com.icm.biometric_zone_gate_api.websocket.utils.DeviceCommandScheduler;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DeviceUserReplicaScheduler {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final DeviceSessionManager sessionManager;
    private final SetUserInfoCommandSender setUserInfoCommandSender;
    private final SetUserInfoReplicaDispatcher replicaDispatcher;
    private final DeviceCommandScheduler deviceCommandScheduler;

    /**
     * Corre cada 3 segundos y revisa si hay credenciales pendientes de replicar
     * hacia dispositivos conectados.
     */
    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void processPendingReplicas() {

        var connectedDevices = deviceRepository.findByStatus(DeviceStatus.CONNECTED);
        if (connectedDevices.isEmpty()) return;

        for (DeviceModel device : connectedDevices) {

            String sn = device.getSn();
            if (sn == null || sn.isBlank()) continue;

            if (!replicaDispatcher.hasPending(sn)) continue;

            WebSocketSession session = sessionManager.getSessionBySn(sn);
            if (session == null || !session.isOpen()) {
                continue; // no está conectado todavía, se reintentará
            }

            Optional<PendingReplica> optPending = replicaDispatcher.poll(sn);
            if (optPending.isEmpty()) continue;

            PendingReplica pending = optPending.get();
            int enrollId = pending.getEnrollId();
            int backupNum = pending.getBackupNum();

            // Buscar usuario y credencial correspondientes
            Optional<UserModel> userOpt = userRepository.findByEnrollId(enrollId);
            if (userOpt.isEmpty()) {
                System.err.printf("⚠ No se encontró usuario con enrollId=%d para replicar en %s%n", enrollId, sn);
                continue;
            }

            UserModel user = userOpt.get();
            Optional<UserCredentialModel> credOpt = userCredentialRepository.findByUserIdAndBackupNum(user.getId(), backupNum);
            if (credOpt.isEmpty()) {
                System.err.printf("⚠ No se encontró credencial backupNum=%d para usuario %s%n", backupNum, user.getName());
                continue;
            }

            UserCredentialModel cred = credOpt.get();

            // Enviar comando al dispositivo
            try {
                String safeName = (user.getName() != null) ? user.getName().replace("\"", "\\\"") : "";
                int adminLevel = (user.getAdminLevel() != null) ? user.getAdminLevel() : 0;

                deviceCommandScheduler.schedule(() -> {
                    try {
                        setUserInfoCommandSender.sendSetUserInfoCommand(
                                session,
                                enrollId,
                                safeName,
                                backupNum,
                                adminLevel,
                                cred.getRecord()
                        );
                        System.out.printf("📡 Replica enviada → dev=%s, enrollId=%d, backupNum=%d%n",
                                sn, enrollId, backupNum);
                    } catch (Exception ex) {
                        System.err.printf("❌ Error enviando réplica a %s: %s%n", sn, ex.getMessage());
                    }
                }, 50L);

            } catch (Exception e) {
                System.err.printf("❌ Error general replicando a %s: %s%n", sn, e.getMessage());
            }
        }
    }
}
