package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.enums.CredentialType;
import com.icm.biometric_zone_gate_api.models.CompanyModel;
import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.models.UserCredentialModel;
import com.icm.biometric_zone_gate_api.models.UserModel;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.repositories.UserCredentialRepository;
import com.icm.biometric_zone_gate_api.repositories.UserRepository;
import com.icm.biometric_zone_gate_api.websocket.commands.SetUserInfoCommandSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUserInfoResponseHandler {

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final DeviceRepository deviceRepository;
    private final SetUserInfoCommandSender setUserInfoCommandSender;

    public void handleGetUserInfoResponse(JsonNode json, WebSocketSession session) {
        try {
            boolean result = json.path("result").asBoolean(false);

            if (!result) {
                int reason = json.path("reason").asInt();
                System.out.println("Fallo GET USER INFO, reason=" + reason);
                return;
            }

            if (result) {
                int enrollId = json.path("enrollid").asInt();
                int backupNum = json.path("backupnum").asInt();
                int admin = json.path("admin").asInt();
                String name = json.path("name").asText();
                String record = json.path("record").asText();

                System.out.printf("Respuesta GET USER INFO:\n EnrollId=%d, BackupNum=%d, Admin=%d, Name=%s, Record=%s%n",
                        enrollId, backupNum, admin, name, record);

                String sn = (String) session.getAttributes().get("sn");
                if (sn == null) {
                    System.err.println("No se encontró SN asociado a la sesión: " + session.getId());
                    return;
                }


                Optional<DeviceModel> deviceOpt = deviceRepository.findBySn(sn);
                DeviceModel device = deviceOpt.get();
                CompanyModel company = device.getCompany();

                // --- Buscar usuario por nombre ---
                Optional<UserModel> userOpt = userRepository.findByName(name);

                if (userOpt.isEmpty()) {
                    // 🔹 Crear usuario nuevo porque no existe en servidor
                    System.out.println("Usuario no existe en BD, creando: " + name);

                    UserModel newUser = new UserModel();
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

                    System.out.println("✅ Usuario creado y credencial registrada en BD: " + name);
                    return;
                }

                // --- Usuario ya existe, comparar credenciales ---
                UserModel existingUser = userOpt.get();
                Optional<UserCredentialModel> credOpt = userCredentialRepository
                        .findByUserIdAndBackupNum(existingUser.getId(), backupNum);

                if (credOpt.isEmpty()) {
                    // 🔹 No tiene esta credencial en BD → registrar
                    System.out.println("Usuario existente pero sin esta credencial, agregando...");

                    UserCredentialModel cred = new UserCredentialModel();
                    cred.setUser(existingUser);
                    cred.setBackupNum(backupNum);
                    cred.setType(mapBackupNumToType(backupNum));
                    cred.setRecord(record);
                    userCredentialRepository.save(cred);

                    System.out.println("✅ Credencial añadida localmente.");
                } else {
                    UserCredentialModel existingCred = credOpt.get();

                    if (!Objects.equals(existingCred.getRecord(), record)) {
                        // ⚠️ Diferencia detectada → imponer versión del servidor
                        System.out.println("⚠️ Diferencia detectada entre servidor y dispositivo.");
                        System.out.println("→ Imponiendo versión del servidor al dispositivo...");

                        setUserInfoCommandSender.sendSetUserInfoCommand(
                                session,
                                enrollId, // el dispositivo lo necesita igual
                                existingUser.getName(),
                                existingCred.getBackupNum(),
                                existingUser.getAdminLevel(),
                                existingCred.getRecord()
                        );
                    } else {
                        System.out.println("✅ Usuario y credencial ya están sincronizados.");
                    }
                }

            } else {
                int reason = json.path("reason").asInt();
                System.out.println("Fallo GET USER INFO, reason=" + reason);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private CredentialType mapBackupNumToType(int backupNum) {
        if (backupNum >= 0 && backupNum <= 9) return CredentialType.FINGERPRINT;
        if (backupNum == 10) return CredentialType.PASSWORD;
        if (backupNum == 11) return CredentialType.CARD;
        return CredentialType.UNKNOWN;
    }
}
