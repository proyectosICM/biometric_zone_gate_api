package com.icm.biometric_zone_gate_api.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint("/ws")
@RequiredArgsConstructor
public class DeviceWebSocketServer {

    private final DeviceMessageHandler handler;

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Device connected: " + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        handler.handle(message, session);
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Device disconnected: " + session.getId());
    }
}

