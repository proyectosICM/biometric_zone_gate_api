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

            for (JsonNode userNode : json.path("record")) {
                int enrollId = userNode.path("enrollid").asInt();
                int admin = userNode.path("admin").asInt();
                int backupNum = userNode.path("backupnum").asInt();

                System.out.printf("   → Procesando usuario enrollId=%d admin=%d backup=%d%n",
                        enrollId, admin, backupNum);
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
