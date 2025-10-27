package com.icm.biometric_zone_gate_api.websocket.schedulers;

import com.icm.biometric_zone_gate_api.enums.DeviceStatus;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.repositories.DeviceUserAccessRepository;
import com.icm.biometric_zone_gate_api.websocket.DeviceSessionManager;
import com.icm.biometric_zone_gate_api.websocket.commands.SetUserNameCommandSender;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.SetUserNameDispatcher;
import com.icm.biometric_zone_gate_api.websocket.commands.SetUserNameCommandSender.UserRecord;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PendingNameScheduler {

    private final DeviceRepository deviceRepository;
    private final DeviceUserAccessRepository deviceUserAccessRepository;
    private final DeviceSessionManager sessionManager;
    private final SetUserNameCommandSender setUserNameCommandSender;
    private final SetUserNameDispatcher dispatcher;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void retryPendingNameChanges() {

        var connected = deviceRepository.findByStatus(DeviceStatus.CONNECTED);
        if (connected.isEmpty()) return;

        for (var device : connected) {
            final String sn = device.getSn();
            if (sn == null || sn.isBlank()) continue;

            var session = sessionManager.getSessionBySn(sn);
            if (session == null || !session.isOpen()) continue;

            var pendings = deviceUserAccessRepository.findPendingNameSyncWithUser(device.getId());
            if (pendings.isEmpty()) continue;

            for (var access : pendings) {
                // si está marcado para borrar, NO sincronizamos nombre
                if (Boolean.TRUE.equals(access.isPendingDelete())) continue;

                var user = access.getUser();
                if (user == null || user.getId() == null) {
                    // acceso huérfano → limpiar flag para no reintentar
                    System.out.printf("Access sin usuario válido → removiendo pendingNameSync id=%d%n", access.getId());
                    access.setPendingNameSync(false);
                    deviceUserAccessRepository.save(access);
                    continue;
                }

                int enrollId = access.getEnrollId();
                if (enrollId <= 0) continue;

                String name = user.getName() != null ? user.getName() : "";
                try {
                    // Reutilizamos el sender que acepta lista de registros
                    var record = new UserRecord(enrollId, name);
                    setUserNameCommandSender.sendSetUserNameCommand(session, List.of(record));

                    dispatcher.register(sn, enrollId, name);
                    System.out.printf("♻️ Reintentando SETUSERNAME (sn=%s, enrollId=%d, name='%s')%n",
                            sn, enrollId, name);
                } catch (Exception e) {
                    System.err.printf("❌ Error enviando SETUSERNAME (sn=%s, enrollId=%d): %s%n",
                            sn, enrollId, e.getMessage());
                }
            }
        }
    }
}
