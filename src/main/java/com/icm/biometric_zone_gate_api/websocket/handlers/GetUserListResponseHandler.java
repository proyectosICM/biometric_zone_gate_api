package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.enums.CredentialType;
import com.icm.biometric_zone_gate_api.models.*;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.repositories.DeviceUserRepository;
import com.icm.biometric_zone_gate_api.repositories.UserCredentialRepository;
import com.icm.biometric_zone_gate_api.repositories.UserRepository;
import com.icm.biometric_zone_gate_api.services.DeviceService;
import com.icm.biometric_zone_gate_api.websocket.commands.GetUserInfoCommandSender;
import com.icm.biometric_zone_gate_api.websocket.sync.UserSyncRegistry;
import com.icm.biometric_zone_gate_api.websocket.utils.DeviceSchedulers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class GetUserListResponseHandler {
    private static final ZoneId SERVER_TZ = ZoneId.of("America/Lima");
    private static final long TIMEOUT_SECONDS = 60;

    private final DeviceRepository deviceRepository;
    private final GetUserInfoCommandSender getUserInfoCommandSender;
    private final DeviceService deviceService;
    private final UserSyncRegistry userSyncRegistry;
    private final DeviceSchedulers schedulers;


    public void handleGetUserListResponse(JsonNode json, WebSocketSession session) {
        try {
            boolean result = json.path("result").asBoolean(false);
            if (!result) return;

            String sn = (String) session.getAttributes().get("sn");
            if (sn == null) {
                System.err.println("No se encontró SN asociado a la sesión: " + session.getId());
                return;
            }

            int count = json.path("count").asInt(0);

            //System.out.println("📩 Respuesta GET USER LIST recibida desde el dispositivo:");
            //System.out.println(" ├─ Resultado: " + (result ? "ÉXITO" : "FALLO"));
            //System.out.println(" ├─ Cantidad de usuarios: " + count);

            if (count == 0) {
                deviceRepository.findBySn(sn).ifPresent(dev ->
                        deviceService.markLastUserSync(dev.getId(), ZonedDateTime.now(SERVER_TZ))
                );
                return;
            }

            userSyncRegistry.start(sn, count);
            var tracker = userSyncRegistry.get(sn);
            tracker.startedAt = ZonedDateTime.now(SERVER_TZ);

            tracker.timeoutFuture = schedulers.ses.schedule(() -> {
                try {
                    deviceRepository.findBySn(sn).ifPresent(dev ->
                            deviceService.markLastUserSync(dev.getId(), ZonedDateTime.now(SERVER_TZ))
                    );
                    System.out.printf("⚠️ Timeout getuserinfo: SN=%s, esperados=%d, pendientes=%d%n",
                            sn, tracker.expected, tracker.pending.get());
                } finally {
                    userSyncRegistry.clear(sn);
                }
            }, TIMEOUT_SECONDS, TimeUnit.SECONDS);

            for (JsonNode userNode : json.path("record")) {
                int enrollId  = userNode.path("enrollid").asInt();
                int admin = userNode.path("admin").asInt();
                int backupNum = userNode.path("backupnum").asInt();
                getUserInfoCommandSender.sendGetUserInfoCommand(session, enrollId, backupNum);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
