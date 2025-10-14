package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetUserListResponseHandler {

    public void handleGetUserListResponse(JsonNode json) {
        try {
            boolean result = json.path("result").asBoolean(false);
            int count = json.path("count").asInt(0);

            System.out.println("📩 Respuesta GET USER LIST recibida desde el dispositivo:");
            System.out.println(" ├─ Resultado: " + (result ? "ÉXITO ✅" : "FALLO ❌"));
            System.out.println(" ├─ Cantidad de usuarios: " + count);

            if (result && count > 0) {
                for (JsonNode userNode : json.path("record")) {
                    int enrollId = userNode.path("enrollid").asInt();
                    int admin = userNode.path("admin").asInt();
                    int backupNum = userNode.path("backupnum").asInt();

                    System.out.printf("   → User: enrollId=%d, admin=%d, backupNum=%d%n", enrollId, admin, backupNum);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
