package com.icm.biometric_zone_gate_api.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mantiene un mapa de SN -> sesión activa y permite manejar múltiples conexiones.
 */
@Component
public class DeviceSessionManager {

    private final ConcurrentHashMap<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    /**
     * Registra una sesión de dispositivo por su SN.
     * Si ya existía otra sesión, la cierra antes de reemplazarla.
     */
    /*
    public void registerSession(String sn, WebSocketSession session) {
        WebSocketSession existing = sessionMap.put(sn, session);
        if (existing != null && existing.isOpen()) {
            try {
                System.out.println("🔌 Cerrando sesión previa del SN " + sn);
                existing.close();
            } catch (Exception e) {
                System.err.println("⚠ Error cerrando sesión previa: " + e.getMessage());
            }
        }
        System.out.println("✅ Sesión registrada para SN " + sn + ", ID: " + session.getId());
    }
     */
    public void registerSession(String sn, WebSocketSession session) {
        sessionMap.put(sn, session);
        System.out.println("✅ Sesión registrada (puente o directa) para SN " + sn + ", ID: " + session.getId());
    }

    /**
     * Elimina la sesión de un SN.
     * Solo elimina si la sesión actual coincide con la registrada (evita eliminar otra sesión nueva).
     */
    public void removeSession(String sn, WebSocketSession session) {
        sessionMap.computeIfPresent(sn, (key, value) -> {
            if (value == session) {
                System.out.println("❌ Sesión eliminada para SN " + sn + ", ID: " + session.getId());
                return null; // eliminar
            }
            return value; // mantener si no coincide
        });
    }

    /**
     * Obtiene la sesión activa por SN.
     */
    public WebSocketSession getSessionBySn(String sn) {
        return sessionMap.get(sn);
    }

    /**
     * Verifica si un dispositivo está conectado.
     */
    public boolean isConnected(String sn) {
        return Optional.ofNullable(sessionMap.get(sn))
                .map(WebSocketSession::isOpen)
                .orElse(false);
    }

    /**
     * Retorna todos los SN conectados actualmente.
     */
    public ConcurrentHashMap.KeySetView<String, WebSocketSession> getAllConnectedSNs() {
        return sessionMap.keySet();
    }
}
