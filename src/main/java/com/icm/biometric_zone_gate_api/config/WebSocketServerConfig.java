package com.icm.biometric_zone_gate_api.config;

import com.icm.biometric_zone_gate_api.websocket.DeviceMessageHandler;
import com.icm.biometric_zone_gate_api.websocket.DeviceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketServerConfig implements WebSocketConfigurer {

    private final DeviceMessageHandler deviceMessageHandler;
    private final DeviceWebSocketHandler deviceWebSocketHandler;
/*
    public WebSocketServerConfig(DeviceMessageHandler deviceMessageHandler) {
        this.deviceMessageHandler = deviceMessageHandler;
    }
*/
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(deviceWebSocketHandler, "/")
                .setAllowedOrigins("*");
        registry.addHandler(deviceWebSocketHandler, "/ws")
                .setAllowedOrigins("*");
    }
}
