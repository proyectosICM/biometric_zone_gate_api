package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.websocket.commands.SetDevInfoCommandSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;

/**
 * Maneja la respuesta del dispositivo al comando "getdevinfo".
 */
@Component
@RequiredArgsConstructor
public class GetDevInfoResponseHandler {

    private final SetDevInfoCommandSender setDevInfoCommandSender;
    private final DeviceRepository deviceRepository;

    public void handleGetDevInfoResponse(JsonNode json, WebSocketSession session) {
        try {
            boolean result = json.path("result").asBoolean(false);

            if (result) {
                int deviceId = json.path("deviceid").asInt();
                int language = json.path("language").asInt();
                int volume = json.path("volume").asInt();
                int screensaver = json.path("screensaver").asInt();
                int verifymode = json.path("verifymode").asInt();
                int sleep = json.path("sleep").asInt();
                int userfpnum = json.path("userfpnum").asInt();
                int loghint = json.path("loghint").asInt();
                int reverifytime = json.path("reverifytime").asInt();

                System.out.println("✅ Respuesta GETDEVINFO recibida correctamente:");
                System.out.printf(
                        """
                        Parámetros del terminal:
                        - deviceid: %d
                        - language: %d
                        - volume: %d
                        - screensaver: %d
                        - verifymode: %d
                        - sleep: %d
                        - userfpnum: %d
                        - loghint: %d
                        - reverifytime: %d
                        """,
                        deviceId, language, volume, screensaver, verifymode,
                        sleep, userfpnum, loghint, reverifytime
                );

                // Obtener el SN (serial number) de la sesión
                String sn = (String) session.getAttributes().get("sn");
                if (sn == null) {
                    System.out.println("⚠️ No se encontró el SN asociado a la sesión WebSocket.");
                    return;
                }

                // 🔹 Actualizar la base de datos
                Optional<DeviceModel> deviceOpt = deviceRepository.findBySn(sn);
                if (deviceOpt.isPresent()) {
                    DeviceModel device = deviceOpt.get();
                    device.setLanguage(language);
                    device.setVolume(volume);
                    device.setUserfpnum(userfpnum);

                    deviceRepository.save(device);
                    System.out.println("💾 Dispositivo actualizado con parámetros reales desde GETDEVINFO: " + sn);
                } else {
                    System.out.println("⚠️ No se encontró un dispositivo con SN=" + sn + " en la base de datos.");
                }
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("Falló GETDEVINFO. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("Error al procesar respuesta de GETDEVINFO: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
