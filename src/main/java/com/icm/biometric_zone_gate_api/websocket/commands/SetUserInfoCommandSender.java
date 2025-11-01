package com.icm.biometric_zone_gate_api.websocket.commands;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Simula el envío del comando "setuserinfo" al dispositivo.
 * Sirve para descargar un usuario (huella, password o tarjeta RFID).
 */
@Component
public class SetUserInfoCommandSender {

    /**
     * Envía el comando setuserinfo con los parámetros indicados.
     *
     * @param session WebSocketSession activa con el dispositivo.
     * @param enrollId ID del usuario en el dispositivo.
     * @param name Nombre del usuario.
     * @param backupNum Tipo de credencial (0–9: huella, 10: password, 11: RFID).
     * @param admin Nivel de admin (0 usuario normal, 1 admin).
     * @param record Datos del registro (plantilla de huella, contraseña o número de tarjeta).
     */
    public void sendSetUserInfoCommand(WebSocketSession session,
                                       int enrollId,
                                       String name,
                                       int backupNum,
                                       int admin,
                                       String record) {
        try {

            String recordField;
            if (backupNum == 10) {
                // Validar que sea numérico; si no, lanza para evitar JSON inválido en el terminal
                if (!isNumeric(record)) {
                    throw new IllegalArgumentException("Para backupnum=10 (password) 'record' debe ser numérico. Valor: " + record);
                }
                recordField = String.format("\"record\":%s", record); // sin comillas
            } else {
                recordField = String.format("\"record\":\"%s\"", escapeJson(record)); // con comillas
            }


            String message = String.format(
                    "{\"cmd\":\"setuserinfo\",\"enrollid\":%d,\"name\":\"%s\",\"backupnum\":%d,\"admin\":%d,\"record\":\"%s\"}",
                    enrollId, name, backupNum, admin, record
            );

            System.out.println("Enviando comando SET USER INFO al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));

        } catch (Exception e) {
            System.err.println("Error al enviar setuserinfo: " + e.getMessage());
        }
    }

    private boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
