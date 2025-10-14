package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetUserInfoResponseHandler {

    public void handleGetUserInfoResponse(JsonNode json) {
        try {
            boolean result = json.path("result").asBoolean(false);

            if (result) {
                int enrollId = json.path("enrollid").asInt();
                int backupNum = json.path("backupnum").asInt();
                int admin = json.path("admin").asInt();
                String name = json.path("name").asText();
                String record = json.path("record").asText();

                System.out.printf("Respuesta GET USER INFO:\n EnrollId=%d, BackupNum=%d, Admin=%d, Name=%s, Record=%s%n",
                        enrollId, backupNum, admin, name, record);
            } else {
                int reason = json.path("reason").asInt();
                System.out.println("Fallo GET USER INFO, reason=" + reason);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
