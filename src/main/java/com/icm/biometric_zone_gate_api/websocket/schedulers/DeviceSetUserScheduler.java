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
import com.icm.biometric_zone_gate_api.websocket.dispatchers.SetUserInfoDispatcher;
import com.icm.biometric_zone_gate_api.websocket.utils.DeviceCommandScheduler;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class DeviceSetUserScheduler {
    private final SetUserInfoDispatcher setUserInfoDispatcher;
    private final DeviceRepository deviceRepository;
    private final DeviceUserAccessRepository deviceUserAccessRepository;
    private final DeviceSessionManager sessionManager;
    private final SetUserInfoCommandSender setUserInfoCommandSender;
    private final DeviceCommandScheduler deviceCommandScheduler;

    // Reglas/constantes en un solo lugar
    private static final ZoneId SERVER_TZ = ZoneId.of("America/Lima");
    private static final long STEP_MS = 120L;
    private static final long GAP_BETWEEN_USERS_MS = 200L;
    private static final int  MAX_CREDENTIALS_PER_DEVICE_PER_WINDOW = 2000;
    private static final int  MAX_CREDENTIALS_CATCHUP_OUTSIDE_WINDOW = 50;

    // Evita reenviar múltiples veces dentro de la misma ventana por dispositivo.
    private final ConcurrentHashMap<Long, Integer> lastWindowByDevice = new ConcurrentHashMap<>();

    /**
     * Corre cada minuto, pero sólo actúa si estamos en la ventana [04..06] de cada decena (y 14..16, 24..26, etc.).
     */

    @Scheduled(cron = "0 * * * * *") // en el segundo 0 de cada minuto
    @Transactional
    public void pushUsersToDevicesInWindow() {
        ZonedDateTime now = ZonedDateTime.now(SERVER_TZ);
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

            boolean reachedLimit = false;
            int sentCount = 0;

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

                    if (sentCount >= MAX_CREDENTIALS_PER_DEVICE_PER_WINDOW) {
                        reachedLimit = true;
                        break; // sale del for (cred)
                    }

                    anySent = true;

                    final int backup = cred.getBackupNum();
                    final String name = safeName(user.getName());
                    final int adminLevel = (user.getAdminLevel() != null) ? user.getAdminLevel() : 0;

                    // REGISTRAR EN DISPATCHER (cola de pendientes por device/enrollId)
                    setUserInfoDispatcher.register(device.getSn(), enrollId, backup);

                    // PROGRAMAR EL ENVÍO (se ejecutará luego; si el device cae, quedará pendiente)
                    final long thisDelay = delay;
                    deviceCommandScheduler.schedule(() -> {
                        try {
                            // Opcional: revalidar sesión en tiempo de ejecución
                            WebSocketSession s = sessionManager.getSessionBySn(device.getSn());
                            if (s == null || !s.isOpen()) {
                                // no envíes; quedará pendiente y se reintentará en próxima ventana
                                return;
                            }

                            setUserInfoCommandSender.sendSetUserInfoCommand(
                                    s, enrollId, name, backup, adminLevel, record
                            );
                        } catch (Exception ex) {
                            System.err.printf("❌ Error enviando setuserinfo (dev=%s, user=%d, backup=%d): %s%n",
                                    device.getSn(), user.getId(), backup, ex.getMessage());
                        }
                    }, thisDelay);

                    delay += STEP_MS;
                    sentCount++;
                }
/*
                final long markDelay = delay + 50L;
                deviceCommandScheduler.schedule(() -> {
                    try {
                        access.setSynced(true);
                        deviceUserAccessRepository.save(access);
                    } catch (Exception ex) {
                        System.err.printf("❌ Error marcando synced access %d: %s%n", access.getId(), ex.getMessage());
                    }
                }, markDelay);
    */
                delay += GAP_BETWEEN_USERS_MS;
                if (reachedLimit) break;
            }

            // ✅ SOLO si enviamos algo, marcamos como atendido esta ventana
            if (anySent) {
                lastWindowByDevice.put(device.getId(), windowSlot);
            }
        }
    }



    /** Público: catch-up para UN device, llamado desde RegisterHandler.
     *  - strictWindow=true: solo si estamos en ventana [04..06]
     *  - strictWindow=false: permite mini-lote fuera de ventana (outsideLimit controla el máximo)
     */
    @Transactional
    public void triggerCatchUpForDevice(DeviceModel device, boolean strictWindow) {
        ZonedDateTime now = ZonedDateTime.now(SERVER_TZ);
        int minute = now.getMinute();
        int mod = minute % 10;
        boolean inWindow = (mod >= 4 && mod <= 6);

        int windowSlot = (now.getHour() * 6) + (minute / 10);

        if (strictWindow && !inWindow) {
            return; // respeta ventana estricta
        }

        int outsideLimit = strictWindow ? 0 : MAX_CREDENTIALS_CATCHUP_OUTSIDE_WINDOW;
        pushUsersForSingleDevice(device, /*strictWindow*/inWindow, outsideLimit, now, windowSlot);
    }

    /** Núcleo para un device (reusa la misma lógica del cron) */
    private void pushUsersForSingleDevice(DeviceModel device,
                                          boolean strictWindowOrInWindow,
                                          int outsideLimit,
                                          ZonedDateTime now,
                                          int windowSlot) {
        // Evita reprocesar si ya se atendió esta ventana
        Integer last = lastWindowByDevice.get(device.getId());
        if (strictWindowOrInWindow && last != null && last == windowSlot) return;

        WebSocketSession session = sessionManager.getSessionBySn(device.getSn());
        if (session == null || !session.isOpen()) return;

        var pending = deviceUserAccessRepository
                .findPendingWithUserAndCredentialsAndPendingDeleteFalse(device.getId());
        if (pending.isEmpty()) return;

        System.out.printf("⏫ [%s] Catch-up/Window → device %s (SN=%s), pendientes=%d%n",
                now, device.getName(), device.getSn(), pending.size());

        long delay = 0L;
        boolean anySent = false;
        int sentCount = 0;
        int maxPerThisRun = (outsideLimit > 0) ? outsideLimit : MAX_CREDENTIALS_PER_DEVICE_PER_WINDOW;

        for (DeviceUserAccessModel access : pending) {
            UserModel user = access.getUser();
            if (user == null || user.getCredentials() == null || user.getCredentials().isEmpty()) {
                access.setSynced(true);
                deviceUserAccessRepository.save(access);
                continue;
            }
            final int enrollId = (access.getEnrollId() > 0)
                    ? access.getEnrollId()
                    : (user.getEnrollId() != 0 ? user.getEnrollId() : 0);
            if (enrollId <= 0) continue;

            for (UserCredentialModel cred : user.getCredentials()) {
                if (sentCount >= maxPerThisRun) break;
                final String record = cred.getRecord();
                if (record == null || record.isEmpty()) continue;

                final int backup = cred.getBackupNum();
                final String name = safeName(user.getName());
                final int adminLevel = (user.getAdminLevel() != null) ? user.getAdminLevel() : 0;

                // Registrar pendiente (con tu dedupe interno del dispatcher)
                setUserInfoDispatcher.register(device.getSn(), enrollId, backup);

                final long thisDelay = delay;
                deviceCommandScheduler.schedule(() -> {
                    try {
                        WebSocketSession s = sessionManager.getSessionBySn(device.getSn());
                        if (s == null || !s.isOpen()) return;
                        setUserInfoCommandSender.sendSetUserInfoCommand(
                                s, enrollId, name, backup, adminLevel, record
                        );
                    } catch (Exception ex) {
                        System.err.printf("❌ Error setuserinfo (dev=%s, enroll=%d, backup=%d): %s%n",
                                device.getSn(), enrollId, backup, ex.getMessage());
                    }
                }, thisDelay);

                delay += STEP_MS;
                sentCount++;
                anySent = true;
            }
            delay += GAP_BETWEEN_USERS_MS;
            if (sentCount >= maxPerThisRun) break;
        }

        if (anySent && strictWindowOrInWindow) {
            // Solo “sellamos” ventana si fue en ventana
            lastWindowByDevice.put(device.getId(), windowSlot);
        }
    }

    private String safeName(String name) {
        if (name == null) return "";
        return name.replace("\"", "\\\"");
    }

}
