package com.icm.biometric_zone_gate_api.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketInitializer {

    private final DeviceConnectionManager connectionManager;

    @PostConstruct
    public void init() {
        System.out.println("Initializing WebSocket connections to devices...");
        connectionManager.connectAllDevices();
    }
}
