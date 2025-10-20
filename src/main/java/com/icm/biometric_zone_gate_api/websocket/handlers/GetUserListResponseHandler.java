package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.enums.CredentialType;
import com.icm.biometric_zone_gate_api.models.*;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.repositories.DeviceUserRepository;
import com.icm.biometric_zone_gate_api.repositories.UserCredentialRepository;
import com.icm.biometric_zone_gate_api.repositories.UserRepository;
import com.icm.biometric_zone_gate_api.websocket.commands.GetUserInfoCommandSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUserListResponseHandler {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceUserRepository deviceUserRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final GetUserInfoCommandSender getUserInfoCommandSender;


    public void handleGetUserListResponse(JsonNode json, WebSocketSession session) {
        try {
            boolean result = json.path("result").asBoolean(false);
            int count = json.path("count").asInt(0);

            String sn = (String) session.getAttributes().get("sn");
            if (sn == null) {
                System.err.println("No se encontró SN asociado a la sesión: " + session.getId());
                return;
            }

            //System.out.println("📩 Respuesta GET USER LIST recibida desde el dispositivo:");
            //System.out.println(" ├─ Resultado: " + (result ? "ÉXITO" : "FALLO"));
            //System.out.println(" ├─ Cantidad de usuarios: " + count);

            if (!result) return;

            if (result && count > 0) {
                for (JsonNode userNode : json.path("record")) {
                    int enrollId = userNode.path("enrollid").asInt();
                    int admin = userNode.path("admin").asInt();
                    int backupNum = userNode.path("backupnum").asInt();
                    getUserInfoCommandSender.sendGetUserInfoCommand(session, enrollId, backupNum);

                    System.out.printf("   → User: enrollId=%d, admin=%d, backupNum=%d%n", enrollId, admin, backupNum);
                }
            }

            /*
            Optional<DeviceModel> deviceOpt = deviceRepository.findBySn(sn);
            if (deviceOpt.isEmpty()) {
                System.err.println("Dispositivo no encontrado en BD: " + sn);
                return;
            }

            DeviceModel device = deviceOpt.get();
            CompanyModel company = device.getCompany();
*/
            for (JsonNode userNode : json.path("record")) {
                int enrollId = userNode.path("enrollid").asInt();
                int admin = userNode.path("admin").asInt();
                int backupNum = userNode.path("backupnum").asInt();

                String userName = "User-" + enrollId; // si el nombre real no viene del dispositivo
                System.out.printf("   → Procesando usuario enrollId=%d admin=%d backup=%d%n",
                        enrollId, admin, backupNum);

                // Buscar o crear el usuario por nombre
                /*
                UserModel user = userRepository.findByName(userName)
                        .orElseGet(() -> {
                            UserModel u = new UserModel();
                            u.setName(userName);
                            u.setRole(com.icm.biometric_zone_gate_api.enums.Role.USER);
                            u.setAdminLevel(admin);
                            u.setEnabled(true);
                            u.setCompany(company);
                            // Asignar empresa por defecto si es obligatorio
                            // u.setCompany(defaultCompany);
                            return userRepository.save(u);
                        });

                // Buscar relación DeviceUser (por device y enrollId)
                /*
                DeviceUserModel deviceUser = deviceUserRepository.findByDeviceIdAndEnrollId(device.getId(), enrollId)
                        .orElseGet(() -> {
                            DeviceUserModel du = new DeviceUserModel();
                            //du.setDevice(device);
                            //du.setUser(user);
                            du.setEnrollId(enrollId);
                            du.setAdminLevel(admin);
                            du.setSynced(true);
                            return deviceUserRepository.save(du);
                        });
*/
                // Si ya existía pero cambió adminLevel
                /*
                if (!deviceUser.getAdminLevel().equals(admin)) {
                    deviceUser.setAdminLevel(admin);
                    deviceUserRepository.save(deviceUser);
                }*/

                // Registrar o actualizar credencial asociada
                /*
                CredentialType type = mapBackupNumToType(backupNum);
                if (type != CredentialType.UNKNOWN) {

                    Optional<UserCredentialModel> existing = userCredentialRepository
                            .findByUserIdAndBackupNum(user.getId(), backupNum);

                    if (existing.isEmpty()) {
                        UserCredentialModel cred = new UserCredentialModel();
                        cred.setBackupNum(backupNum);
                        cred.setType(type);
                        cred.setRecord(null); // En este punto el dispositivo solo envía tipo, no datos
                        cred.setUser(user);
                        userCredentialRepository.save(cred);
                        System.out.println("     ✅ Credencial registrada: " + type + " (backupNum=" + backupNum + ")");
                    } else {
                        System.out.println("     ℹ️ Credencial existente: " + type + " (backupNum=" + backupNum + ")");
                    }
                } else {
                    System.out.println("     ⚠️ Tipo de credencial desconocido para backupNum=" + backupNum);
                }*/
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private CredentialType mapBackupNumToType(int backupNum) {
        if (backupNum >= 0 && backupNum <= 9) {
            return CredentialType.FINGERPRINT;
        } else if (backupNum == 10) {
            return CredentialType.PASSWORD;
        } else if (backupNum == 11) {
            return CredentialType.CARD;
        } else {
            return CredentialType.UNKNOWN;
        }
    }
}
