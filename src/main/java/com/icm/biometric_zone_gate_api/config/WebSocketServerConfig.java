package com.icm.biometric_zone_gate_api.config;

import com.icm.biometric_zone_gate_api.websocket.DeviceMessageHandler;
import com.icm.biometric_zone_gate_api.websocket.DeviceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketServerConfig implements WebSocketConfigurer {

    private final DeviceMessageHandler deviceMessageHandler;
    private final DeviceWebSocketHandler deviceWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(deviceWebSocketHandler, "/ws")
                .setAllowedOrigins("*");
    }

    @Bean
    public ServletServerContainerFactoryBean webSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // Los dispositivos pueden enviar plantillas biométricas grandes codificadas en Base64.
        // Aumentamos el buffer permitido para evitar cierres del canal (código 1009) por mensajes largos.
        container.setMaxTextMessageBufferSize(4 * 1024 * 1024); // 4 MB
        container.setMaxBinaryMessageBufferSize(4 * 1024 * 1024);
        return container;
    }
}
