package com.icm.biometric_zone_gate_api.websocket.schedulers;

import com.icm.biometric_zone_gate_api.enums.DeviceStatus;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.websocket.DeviceSessionManager;
import com.icm.biometric_zone_gate_api.websocket.commands.InitSystemCommandSender;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.InitSystemDispatcher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
@RequiredArgsConstructor
public class PendingInitSystemScheduler {

    private final DeviceRepository deviceRepository;
    private final DeviceSessionManager sessionManager;
    private final InitSystemDispatcher dispatcher;
    private final InitSystemCommandSender sender;

    /**
     * Reintenta INIT SYSTEM cada 5s si está en cola y el device está conectado.
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void retryInitSystem() {

        var connectedDevices = deviceRepository.findByStatus(DeviceStatus.CONNECTED);
        if (connectedDevices.isEmpty()) return;

        for (var device : connectedDevices) {
            final String sn = device.getSn();
            if (sn == null || sn.isBlank()) continue;

            // ¿hay pendiente para este device?
            if (!dispatcher.hasPending(sn)) continue;

            WebSocketSession session = sessionManager.getSessionBySn(sn);
            if (session == null || !session.isOpen()) {
                System.out.printf("⚠ Device %s conectado pero sesión no abierta.\n", sn);
                continue;
            }

            try {
                // Re-enviar INIT SYSTEM
                sender.sendInitSystemCommand(session);
                System.out.printf("🔁 Reintentando INIT SYSTEM (sn=%s)\n", sn);
            } catch (Exception e) {
                System.err.printf("❌ Error reintentando INIT SYSTEM (sn=%s): %s\n",
                        sn, e.getMessage());
            }
        }
    }
}
