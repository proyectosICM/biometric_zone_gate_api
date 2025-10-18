package com.icm.biometric_zone_gate_api.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icm.biometric_zone_gate_api.websocket.handlers.GetAllLogResponseHandler;
import com.icm.biometric_zone_gate_api.websocket.handlers.GetNewLogResponseHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
public class DeviceWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DeviceMessageHandler messageHandler;
    private final GetNewLogResponseHandler getNewLogResponseHandler;
    private final GetAllLogResponseHandler getAllLogResponseHandler;

    /*
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        messageHandler.handle(message.getPayload(), session);
    }
*/
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("Device connected: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        System.out.println("Device disconnected: " + session.getId());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        try {
            JsonNode json = objectMapper.readTree(payload);

            String cmd = json.path("cmd").asText(null);
            String ret = json.path("ret").asText(null);

            // 🔍 Si el mensaje es respuesta de getnewlog
            if ("getnewlog".equalsIgnoreCase(cmd) || "getnewlog".equalsIgnoreCase(ret)) {
                getNewLogResponseHandler.handleGetNewLogResponse(json, session);
                return;
            }

            if ("getalllog".equalsIgnoreCase(cmd) || "getalllog".equalsIgnoreCase(ret)) {
                getAllLogResponseHandler.handleGetAllLogResponse(json, session);
                return;
            }

            // 🧩 En caso contrario, pasa al manejador general
            messageHandler.handle(payload, session);

        } catch (Exception e) {
            System.err.println("❌ Error procesando mensaje WebSocket: " + e.getMessage());
            e.printStackTrace();

            // Si el mensaje no es JSON, se pasa directamente al manejador genérico
            messageHandler.handle(payload, session);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        System.err.println("⚠️ Error de transporte WebSocket: " + exception.getMessage());
    }
}
