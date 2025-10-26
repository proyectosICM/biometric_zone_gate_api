package com.icm.biometric_zone_gate_api.websocket.commands;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

/**
 * Envía el comando "setusername" al dispositivo.
 * Permite establecer o actualizar los nombres de usuario en el dispositivo.
 */
@Component
public class SetUserNameCommandSender {

    /**
     * Envía el comando "setusername" al dispositivo.
     *
     * @param session WebSocketSession activa con el dispositivo.
     * @param records Lista de pares (enrollid, name) a enviar. Máximo 50.
     */
    public void sendSetUserNameCommand(WebSocketSession session, List<UserRecord> records) {
        try {
            if (records == null || records.isEmpty()) {
                System.err.println("⚠️ No se enviaron registros en setusername.");
                return;
            }
            if (records.size() > 50) {
                throw new IllegalArgumentException("❌ No se permiten más de 50 registros por paquete.");
            }

            StringBuilder recordArray = new StringBuilder("[");
            for (int i = 0; i < records.size(); i++) {
                UserRecord r = records.get(i);
                recordArray.append(String.format("{\"enrollid\":%d,\"name\":\"%s\"}", r.enrollid, r.name));
                if (i < records.size() - 1) recordArray.append(",");
            }
            recordArray.append("]");

            String message = String.format(
                    "{\"cmd\":\"setusername\",\"count\":%d,\"record\":%s}",
                    records.size(), recordArray
            );

            System.out.println("✉️ Enviando comando SET USERNAME al dispositivo...");
            System.out.println(message);

            session.sendMessage(new TextMessage(message));

        } catch (Exception e) {
            System.err.println("❌ Error al enviar setusername: " + e.getMessage());
        }
    }

    /**
     * Representa un registro individual de usuario para setusername.
     */
    public static class UserRecord {
        public int enrollid;
        public String name;

        public UserRecord(int enrollid, String name) {
            this.enrollid = enrollid;
            this.name = name;
        }
    }
}
