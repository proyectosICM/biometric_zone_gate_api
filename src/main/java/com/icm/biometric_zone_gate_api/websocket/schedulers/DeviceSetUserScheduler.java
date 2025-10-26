package com.icm.biometric_zone_gate_api.websocket.schedulers;

import com.icm.biometric_zone_gate_api.enums.DeviceStatus;
import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.models.DeviceUserAccessModel;
import com.icm.biometric_zone_gate_api.models.UserCredentialModel;
import com.icm.biometric_zone_gate_api.models.UserModel;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.repositories.DeviceUserAccessRepository;
import com.icm.biometric_zone_gate_api.websocket.DeviceSessionManager;
import com.icm.biometric_zone_gate_api.websocket.commands.SetUserInfoCommandSender;
import com.icm.biometric_zone_gate_api.websocket.utils.DeviceCommandScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class DeviceSetUserScheduler {

    private final DeviceRepository deviceRepository;
    private final DeviceUserAccessRepository deviceUserAccessRepository;
    private final DeviceSessionManager sessionManager;
    private final SetUserInfoCommandSender setUserInfoCommandSender;
    private final DeviceCommandScheduler deviceCommandScheduler;

    // Evita reenviar múltiples veces dentro de la misma ventana por dispositivo.
    private final ConcurrentHashMap<Long, Integer> lastWindowByDevice = new ConcurrentHashMap<>();

    /**
     * Corre cada minuto, pero sólo actúa si estamos en la ventana [04..06] de cada decena (y 14..16, 24..26, etc.).
     */
    @Scheduled(cron = "0 * * * * *") // en el segundo 0 de cada minuto
    public void pushUsersToDevicesInWindow() {
        LocalDateTime now = LocalDateTime.now();
        int minute = now.getMinute();
        int mod = minute % 10;

        // Ventana 04..06 de cada decena
        if (mod < 4 || mod > 6) {
            return; // fuera de ventana → no hacemos nada
        }

        // Identificador de ventana por hora (ej. HH*6 + slot)
        int windowSlot = (now.getHour() * 6) + (minute / 10); // cada decena es un slot

        // Dispositivos conectados
        List<DeviceModel> connected = deviceRepository.findByStatus(DeviceStatus.CONNECTED);
        if (connected.isEmpty()) return;

        for (DeviceModel device : connected) {

            // ¿ya atendimos esta ventana para este device?
            Integer last = lastWindowByDevice.get(device.getId());
            if (last != null && last == windowSlot) {
                continue; // ya se procesó en esta ventana
            }

            WebSocketSession session = sessionManager.getSessionBySn(device.getSn());
            if (session == null || !session.isOpen()) {
                continue; // aún no está conectado → reintentará en el próximo minuto dentro de la misma ventana
            }

            // Accesos pendientes de sync (enabled && synced=false)
            List<DeviceUserAccessModel> pending = deviceUserAccessRepository
                    .findPendingWithUserAndCredentialsAndPendingDeleteFalse(device.getId());

            if (pending.isEmpty()) {
                // Nada que enviar todavía → NO marcamos ventana
                continue;
            }

            System.out.printf("⏫ [%s] Enviando %d accesos pendientes a device %s (SN=%s)%n",
                    now, pending.size(), device.getName(), device.getSn());

            long delay = 0L; // vamos escalonando para no saturar
            final long STEP_MS = 120L;
            boolean anySent = false;

            for (DeviceUserAccessModel access : pending) {
                UserModel user = access.getUser();
                if (user == null || user.getCredentials() == null || user.getCredentials().isEmpty()) {
                    // Sin credenciales → marcamos synced para no intentar eternamente
                    access.setSynced(true);
                    deviceUserAccessRepository.save(access);
                    continue;
                }

                final int enrollId = (access.getEnrollId() > 0)
                        ? access.getEnrollId()
                        : (user.getEnrollId() != 0 ? user.getEnrollId() : 0);

                if (enrollId <= 0) {
                    // No lo marcamos synced; se reintentará más tarde
                    continue;
                }

                for (UserCredentialModel cred : user.getCredentials()) {
                    final String record = cred.getRecord();
                    if (record == null || record.isEmpty()) continue;

                    anySent = true; // ✅ ahora sí marcamos que se envió algo

                    final int backup = cred.getBackupNum();
                    final String name = safeName(user.getName());
                    final int adminLevel = (user.getAdminLevel() != null) ? user.getAdminLevel() : 0;

                    deviceCommandScheduler.schedule(() -> {
                        try {
                            setUserInfoCommandSender.sendSetUserInfoCommand(
                                    session, enrollId, name, backup, adminLevel, record
                            );
                        } catch (Exception ex) {
                            System.err.printf("❌ Error enviando setuserinfo (dev=%s, user=%d, backup=%d): %s%n",
                                    device.getSn(), user.getId(), backup, ex.getMessage());
                        }
                    }, delay);

                    delay += STEP_MS;
                }

                final long markDelay = delay + 50L;
                deviceCommandScheduler.schedule(() -> {
                    try {
                        access.setSynced(true);
                        deviceUserAccessRepository.save(access);
                    } catch (Exception ex) {
                        System.err.printf("❌ Error marcando synced access %d: %s%n", access.getId(), ex.getMessage());
                    }
                }, markDelay);

                delay += 200L;
            }

            // ✅ SOLO si enviamos algo, marcamos como atendido esta ventana
            if (anySent) {
                lastWindowByDevice.put(device.getId(), windowSlot);
            }
        }
    }

    private String safeName(String name) {
        if (name == null) return "";
        return name.replace("\"", "\\\"");
    }
}
