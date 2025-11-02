package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.enums.CredentialType;
import com.icm.biometric_zone_gate_api.models.*;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.repositories.DeviceUserAccessRepository;
import com.icm.biometric_zone_gate_api.repositories.UserCredentialRepository;
import com.icm.biometric_zone_gate_api.repositories.UserRepository;
import com.icm.biometric_zone_gate_api.services.DeviceService;
import com.icm.biometric_zone_gate_api.websocket.commands.SetUserInfoCommandSender;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.SetUserInfoReplicaDispatcher;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.SetUserNameReplicaDispatcher;
import com.icm.biometric_zone_gate_api.websocket.sync.UserSyncRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUserInfoResponseHandler {

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final DeviceRepository deviceRepository;
    private final SetUserInfoCommandSender setUserInfoCommandSender;
    private final DeviceUserAccessRepository deviceUserAccessRepository;
    private final SetUserInfoReplicaDispatcher replicaDispatcher;
    private final SetUserNameReplicaDispatcher   nameReplicaDispatcher;

    private final DeviceService deviceService;
    private final UserSyncRegistry userSyncRegistry;

    private static final ZoneId SERVER_TZ = ZoneId.of("America/Lima");

    public void handleGetUserInfoResponse(JsonNode json, WebSocketSession session) {
        try {
            boolean result = json.path("result").asBoolean(false);

            if (!result) {
                int reason = json.path("reason").asInt();
                System.out.println("Fallo GET USER INFO, reason=" + reason);
                String snFail = (String) session.getAttributes().get("sn");
                if (snFail != null) {
                    finishIfDone(snFail);
                }
                return;
            }

            String sn = (String) session.getAttributes().get("sn");
            if (sn == null) {
                System.err.println("No se encontró SN asociado a la sesión: " + session.getId());
                return;
            }

            int enrollId = json.path("enrollid").asInt();
            int backupNum = json.path("backupnum").asInt();
            int admin = json.path("admin").asInt();
            String name = json.path("name").asText();
            //String record = json.path("record").asText();
            String record = extractRecord(json.get("record"), backupNum);

            System.out.printf("GETUSERINFO OK → EnrollId=%d, BackupNum=%d, Admin=%d, Name=%s%n",
                    enrollId, backupNum, admin, name);

            Optional<DeviceModel> deviceOpt = deviceRepository.findBySn(sn);
            if (deviceOpt.isEmpty()) {
                System.err.println("Dispositivo no encontrado en BD para SN=" + sn);
                finishIfDone(sn);
                return;
            }

            DeviceModel device = deviceOpt.get();
            CompanyModel company = device.getCompany();

            // Buscar usuario por nombre
            Optional<UserModel> userOpt = userRepository.findByEnrollIdAndCompany(enrollId, company);

            if (userOpt.isEmpty()) {
                // CREAR usuario porque NO existe
                System.out.println("Usuario no existe en BD, creando: " + name);

                UserModel newUser = new UserModel();
                newUser.setEnrollId(enrollId);
                newUser.setName(name);
                newUser.setAdminLevel(admin);
                newUser.setEnabled(true);
                newUser.setCompany(company);
                newUser = userRepository.save(newUser);

                UserCredentialModel newCred = new UserCredentialModel();
                newCred.setUser(newUser);
                newCred.setBackupNum(backupNum);
                newCred.setType(mapBackupNumToType(backupNum));
                newCred.setRecord(record);
                userCredentialRepository.save(newCred);

                DeviceUserAccessModel access = new DeviceUserAccessModel();
                access.setDevice(device);
                access.setUser(newUser);
                access.setEnrollId(enrollId);
                //access.setWeekZone(0);
                //access.setGroupNumber(0);
                //access.setStartTime(ZonedDateTime.now());
                access.setEnabled(true);
                access.setSynced(true);
                deviceUserAccessRepository.save(access);

                finishIfDone(sn);
                return;
            }

            // Si ya existe:
            UserModel existingUser = userOpt.get();

            if (!Objects.equals(existingUser.getName(), name)) {
                System.out.printf("✏️ Cambio de nombre detectado: '%s' → '%s'%n",
                        existingUser.getName(), name);

                existingUser.setName(name);
                userRepository.save(existingUser);

                // Replicar a otros dispositivos
                deviceUserAccessRepository.findByUserId(existingUser.getId())
                        .forEach(link -> {
                            DeviceModel target = link.getDevice();
                            if (!Objects.equals(target.getSn(), sn)) {
                                nameReplicaDispatcher.register(target.getSn(), enrollId, name);
                                link.setPendingNameSync(true);
                                deviceUserAccessRepository.save(link);
                            }
                        });
            }

            deviceUserAccessRepository.findByUserIdAndDeviceId(existingUser.getId(), device.getId())
                    .ifPresentOrElse(
                            access -> {
                                access.setSynced(true);
                                deviceUserAccessRepository.save(access);
                            },
                            () -> {
                                DeviceUserAccessModel newAccess = new DeviceUserAccessModel();
                                newAccess.setDevice(device);
                                newAccess.setUser(existingUser);
                                newAccess.setEnrollId(enrollId);
                                newAccess.setEnabled(true);
                                newAccess.setSynced(true);
                                deviceUserAccessRepository.save(newAccess);
                            }
                    );

            Optional<UserCredentialModel> credOpt =
                    userCredentialRepository.findByUserIdAndBackupNum(existingUser.getId(), backupNum);

            if (credOpt.isEmpty()) {
                System.out.println("Nueva credencial detectada → guardando y marcando replicación");
                UserCredentialModel newCred = new UserCredentialModel();
                newCred.setUser(existingUser);
                newCred.setBackupNum(backupNum);
                newCred.setType(mapBackupNumToType(backupNum));
                newCred.setRecord(record);
                userCredentialRepository.save(newCred);

                registerReplicaForOtherDevices(existingUser, sn, enrollId, backupNum);

            } else {
                UserCredentialModel existingCred = credOpt.get();
                if (!Objects.equals(existingCred.getRecord(), record)) {
                    existingCred.setRecord(record);
                    userCredentialRepository.save(existingCred);
                    registerReplicaForOtherDevices(existingUser, sn, enrollId, backupNum);
                }
            }
            finishIfDone(sn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Decrementa el pendiente; si llega a 0 → cancela timeout, marca lastUserSync y limpia el registro. */
    private void finishIfDone(String sn) {
        boolean done = userSyncRegistry.decrementAndIsDone(sn);
        if (done) {
            var tracker = userSyncRegistry.get(sn);
            if (tracker != null && tracker.timeoutFuture != null) {
                tracker.timeoutFuture.cancel(false);
            }
            deviceRepository.findBySn(sn).ifPresent(dev ->
                    deviceService.markLastUserSync(dev.getId(), ZonedDateTime.now(SERVER_TZ))
            );
            userSyncRegistry.clear(sn);
        }
    }

    private void registerReplicaForOtherDevices(UserModel user, String originSn, int enrollId, int backupNum) {
        var accesses = deviceUserAccessRepository.findByUserId(user.getId());
        accesses.stream()
                .map(DeviceUserAccessModel::getDevice)
                .filter(d -> !Objects.equals(d.getSn(), originSn))
                .forEach(device -> {
                    replicaDispatcher.register(device.getSn(), enrollId, backupNum);
                    deviceUserAccessRepository.findByUserIdAndDeviceId(user.getId(), device.getId())
                            .ifPresent(access -> {
                                access.setSynced(false);
                                deviceUserAccessRepository.save(access);
                            });
                });
    }

    private CredentialType mapBackupNumToType(int backupNum) {
        if (backupNum >= 0 && backupNum <= 9) return CredentialType.FINGERPRINT;
        if (backupNum == 10) return CredentialType.PASSWORD;
        if (backupNum == 11) return CredentialType.CARD;
        if (backupNum == 50) return CredentialType.PHOTO;
        return CredentialType.UNKNOWN;
    }

    private String extractRecord(JsonNode recordNode, int backupNum) {
        if (recordNode == null || recordNode.isNull()) return "";

        // PASSWORD
        if (backupNum == 10) {
            // Acepta número o texto; devuelve siempre dígitos sin espacios
            String s = recordNode.isNumber() ? recordNode.asText() : recordNode.asText();
            s = s.trim();
            if (!s.chars().allMatch(Character::isDigit)) {
                throw new IllegalArgumentException("Record PASSWORD no es numérico: " + s);
            }
            return s; // guardar como string de dígitos
        }

        return recordNode.asText();
    }
}
