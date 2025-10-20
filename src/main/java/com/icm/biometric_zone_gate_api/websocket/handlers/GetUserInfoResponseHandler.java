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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetUserInfoResponseHandler {

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final DeviceRepository deviceRepository;

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

                // Buscar o crear usuario
                UserModel user = userRepository.findByName(name)
                        .orElseGet(() -> {
                            UserModel u = new UserModel();
                            u.setName(name);
                            u.setAdminLevel(admin);
                            u.setEnabled(true);
                            u.setCompany(company);
                            return userRepository.save(u);
                        });

                userCredentialRepository.findByUserIdAndBackupNum(user.getId(), backupNum)
                        .orElseGet(() -> {
                            UserCredentialModel cred = new UserCredentialModel();
                            cred.setUser(user);
                            cred.setBackupNum(backupNum);
                            cred.setRecord(record);
                            cred.setType(mapBackupNumToType(backupNum));
                            return userCredentialRepository.save(cred);
                        });

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
