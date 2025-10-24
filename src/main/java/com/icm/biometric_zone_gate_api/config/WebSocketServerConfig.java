package com.icm.biometric_zone_gate_api.config;

import com.icm.biometric_zone_gate_api.websocket.DeviceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.ServletContext;
import jakarta.websocket.server.ServerContainer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
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

    private final DeviceWebSocketHandler deviceWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(deviceWebSocketHandler, "/ws")
                .setAllowedOrigins("*");
    }

    @Bean
    public ServletContextInitializer webSocketContainerCustomizer() {
        return new ServletContextInitializer() {
            @Override
            public void onStartup(ServletContext servletContext) {
                Object attribute = servletContext.getAttribute("jakarta.websocket.server.ServerContainer");
                if (attribute instanceof ServerContainer container) {
                    // Los dispositivos pueden enviar plantillas biométricas grandes codificadas en Base64.
                    // Aumentamos el buffer permitido para evitar cierres del canal (código 1009) por mensajes largos.
                    container.setDefaultMaxTextMessageBufferSize(4 * 1024 * 1024); // 4 MB
                    container.setDefaultMaxBinaryMessageBufferSize(4 * 1024 * 1024);
                }
            }
        };
    }
}
