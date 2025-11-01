package com.icm.biometric_zone_gate_api.websocket.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class SetUserInfoCommandSender {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public void sendSetUserInfoCommand(WebSocketSession session,
                                       int enrollId,
                                       String name,
                                       int backupNum,
                                       int admin,
                                       String record) {
        try {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("cmd", "setuserinfo");
            root.put("enrollid", enrollId);
            root.put("name", name);
            root.put("backupnum", backupNum);
            root.put("admin", admin);

            boolean mustBeNumeric = (backupNum == 10); // ó (backupNum == 10 || backupNum == 11)
            if (mustBeNumeric) {
                if (!isNumeric(record)) {
                    throw new IllegalArgumentException(
                            "Para backupnum=" + backupNum + " 'record' debe ser numérico. Valor: " + record
                    );
                }
                // como número (usa Long para tarjetas largas; Jackson lo serializa como número JSON)
                root.put("record", Long.parseLong(record));
            } else {
                root.put("record", record);
            }

            String json = MAPPER.writeValueAsString(root);

            System.out.println("Enviando comando SET USER INFO al dispositivo...");
            System.out.println(json);

            session.sendMessage(new TextMessage(json));

        } catch (Exception e) {
            System.err.println("Error al enviar setuserinfo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }
}
