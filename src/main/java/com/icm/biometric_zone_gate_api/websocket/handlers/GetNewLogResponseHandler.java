package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.icm.biometric_zone_gate_api.websocket.commands.GetNewLogCommandSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
@RequiredArgsConstructor
public class GetNewLogResponseHandler {

    private final GetNewLogCommandSender getNewLogCommandSender;

    public void handleGetNewLogResponse(JsonNode json, WebSocketSession session) {
        try {
            boolean result = json.path("result").asBoolean(false);
            String ret = json.path("ret").asText("");

            if (!"getnewlog".equalsIgnoreCase(ret)) {
                System.out.println("⚠️ Respuesta ignorada: no corresponde a 'getnewlog'.");
                return;
            }

            if (!result) {
                int reason = json.path("reason").asInt(-1);
                System.out.printf("⚠️ GETNEWLOG falló. reason=%d%n", reason);
                return;
            }

            int count = json.path("count").asInt(0);
            int from = json.path("from").asInt(0);
            int to = json.path("to").asInt(0);

            System.out.printf("✅ GETNEWLOG respuesta: count=%d, from=%d, to=%d%n", count, from, to);

            // 📋 Si hay registros, procesarlos
            if (count > 0) {
                if (json.has("record") && json.get("record").isArray()) {
                    ArrayNode records = (ArrayNode) json.get("record");
                    System.out.println("📋 Registros recibidos (" + records.size() + "):");
                    for (JsonNode record : records) {
                        int enrollId = record.path("enrollid").asInt();
                        String time = record.path("time").asText();
                        int mode = record.path("mode").asInt();
                        int inout = record.path("inout").asInt();
                        int event = record.path("event").asInt();
                        System.out.printf(" - ID:%d | Time:%s | Mode:%d | InOut:%d | Event:%d%n",
                                enrollId, time, mode, inout, event);
                    }
                }

                // 🔁 Solo pedimos más si realmente quedan logs
                System.out.println("⏳ Solicitando siguiente paquete de logs...");
                getNewLogCommandSender.sendGetNewLogCommand(session, false);

            } else {
                // ✅ count == 0 → detener por completo
                System.out.println("📭 No hay más registros nuevos. Fin del ciclo GETNEWLOG.");
                return;
            }

        } catch (Exception e) {
            System.err.println("❌ Error al procesar respuesta de GETNEWLOG: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
