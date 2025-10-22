package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.models.UserModel;
import com.icm.biometric_zone_gate_api.services.DeviceService;
import com.icm.biometric_zone_gate_api.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Maneja la respuesta al comando "getuserlock".
 * Contiene los parámetros de acceso de un usuario (zona, grupo, tiempo válido, etc.).
 */
@Component
@RequiredArgsConstructor
public class GetUserLockResponseHandler {

    private final UserService userService;
    private final DeviceService deviceService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void handleGetUserLockResponse(JsonNode json, WebSocketSession session) {
        try {
            String ret = json.path("ret").asText("");
            boolean result = json.path("result").asBoolean(false);

            if (!"getuserlock".equalsIgnoreCase(ret)) {
                System.out.println("⚠️ Respuesta ignorada: no corresponde a 'getuserlock'.");
                return;
            }

            if (!result) {
                int reason = json.path("reason").asInt(-1);
                System.out.printf("❌ GETUSERLOCK falló. reason=%d%n", reason);
                return;
            }

            int enrollId = json.path("enrollid").asInt();
            int weekzone = json.path("weekzone").asInt();
            int group = json.path("group").asInt();
            String startTimeStr = json.path("starttime").asText();
            String endTimeStr = json.path("endtime").asText();

            LocalDateTime startTime = LocalDateTime.parse(startTimeStr, FORMATTER);
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, FORMATTER);

            System.out.printf("""
                    ✅ GETUSERLOCK exitoso:
                      - enrollId: %d
                      - weekzone: %d
                      - group: %d
                      - starttime: %s
                      - endtime: %s
                    """, enrollId, weekzone, group, startTimeStr, endTimeStr);

            // Obtener el SN del dispositivo
            String sn = (String) session.getAttributes().get("sn");
            if (sn == null) {
                System.err.println("❌ No se encontró SN en la sesión " + session.getId());
                return;
            }

            Optional<DeviceModel> optDevice = deviceService.getDeviceBySn(sn);
            if (optDevice.isEmpty()) {
                System.err.println("❌ Dispositivo no encontrado con SN=" + sn);
                return;
            }

            // Buscar el usuario por su enrollId
            Optional<UserModel> optUser = userService.getUserById((long) enrollId);
            if (optUser.isEmpty()) {
                System.err.println("⚠️ Usuario no encontrado con enrollId=" + enrollId);
                return;
            }

            UserModel user = optUser.get();

            // TODO: Guardar o actualizar en base de datos si quieres persistir los parámetros de acceso
            // Por ejemplo:
            // user.setWeekzone(weekzone);
            // user.setAccessGroup(group);
            // user.setAccessStartTime(startTime);
            // user.setAccessEndTime(endTime);
            // userService.updateUser(user);

            System.out.printf("💾 Parámetros de acceso actualizados (temporalmente) para usuario: %s%n", user.getUsername());

        } catch (Exception e) {
            System.err.println("❌ Error al procesar respuesta de GETUSERLOCK: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
