package com.icm.biometric_zone_gate_api.websocket.commands;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Envía el comando "setdevlock" al dispositivo.
 * Este comando configura los parámetros de acceso (control de puerta, alarmas, zonas horarias, etc.)
 */
@Component
public class SetDevLockCommandSender {

    public void sendSetDevLockCommand(WebSocketSession session) {
        try {
            // 🧩 Comando mínimo (puede ampliarse luego con parámetros reales)
            String message = """
                {
                    "cmd": "setdevlock",
                    "opendelay": 5,
                    "doorsensor": 0,
                    "alarmdelay": 0,
                    "threat": 0,
                    "InputAlarm": 0,
                    "antpass": 0,
                    "interlock": 0,
                    "mutiopen": 0,
                    "tryalarm": 0,
                    "tamper": 0,
                    "wgformat": 0,
                    "wgoutput": 0,
                    "cardoutput": 0
                }
                """;

            System.out.println("📤 Enviando comando SETDEVLOCK al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));

        } catch (Exception e) {
            System.err.println("❌ Error al enviar SETDEVLOCK: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
