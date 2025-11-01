package com.icm.biometric_zone_gate_api.websocket.schedulers;

import com.icm.biometric_zone_gate_api.enums.DeviceStatus;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.websocket.DeviceSessionManager;
import com.icm.biometric_zone_gate_api.websocket.commands.OpenDoorCommandSender;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.OpenDoorDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
@RequiredArgsConstructor
public class PendingOpenDoorScheduler {

    private final DeviceRepository deviceRepository;
    private final DeviceSessionManager sessionManager;
    private final OpenDoorCommandSender sender;
    private final OpenDoorDispatcher dispatcher;

    @Scheduled(fixedDelay = 2000)
    public void retryOpenDoor() {
        var connected = deviceRepository.findByStatus(DeviceStatus.CONNECTED);
        if (connected.isEmpty()) return;

        connected.stream()
                .filter(d -> d.getSn() != null && !d.getSn().isBlank())
                .forEach(device -> {
                    String sn = device.getSn();
                    if (!dispatcher.hasPending(sn)) return;

                    var optHead = dispatcher.peek(sn);
                    if (optHead.isEmpty()) return;

                    var head = optHead.get();

                    // ¿Expirado o sin intentos restantes?
                    if (head.isExpired()) {
                        dispatcher.dropHead(sn, "expired_1min_window");
                        return;
                    }
                    if (!head.attemptsLeft()) {
                        dispatcher.dropHead(sn, "max_attempts_reached");
                        return;
                    }
                    if (!head.canRetryNow()) {
                        return; // aún en cooldown
                    }

                    WebSocketSession session = sessionManager.getSessionBySn(sn);
                    if (session == null || !session.isOpen()) {
                        // No enviamos ahora; se reintentará en el siguiente tick cuando conecte.
                        return;
                    }

                    try {
                        sender.sendOpenDoorCommand(session);
                        head.markSent();
                        System.out.printf("♻️ Reintentando OPENDOOR (sn=%s, door=%s, attempt=%d)%n",
                                sn, head.getDoorNum(), head.getAttempts());
                    } catch (Exception e) {
                        System.err.printf("❌ Error enviando OPENDOOR (sn=%s): %s%n", sn, e.getMessage());
                    }
                });
    }
}
