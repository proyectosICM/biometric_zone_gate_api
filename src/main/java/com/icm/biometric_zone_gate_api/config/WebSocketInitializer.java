package com.icm.biometric_zone_gate_api.config;

import com.icm.biometric_zone_gate_api.websocket.DeviceWebSocketClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketInitializer {

    private final DeviceWebSocketClient deviceWebSocketClient;

    @PostConstruct
    public void init() {
        try {
            // Cambia la IP por la de tu dispositivo
            deviceWebSocketClient.connect("192.168.1.100");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
