package com.icm.biometric_zone_gate_api.websocket;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class DeviceWebSocketHandler extends TextWebSocketHandler {

    private final DeviceMessageHandler messageHandler;

    public DeviceWebSocketHandler(DeviceMessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        messageHandler.handle(message.getPayload(), session);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("Device connected: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        System.out.println("Device disconnected: " + session.getId());
    }
}
