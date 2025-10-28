package com.icm.biometric_zone_gate_api.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.models.DeviceUserAccessModel;
import com.icm.biometric_zone_gate_api.models.UserModel;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.repositories.DeviceUserAccessRepository;
import com.icm.biometric_zone_gate_api.repositories.UserRepository;
import com.icm.biometric_zone_gate_api.websocket.dispatchers.CleanUserDispatcher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

/**
 * Maneja la respuesta del dispositivo al comando "cleanuser".
 */
@Component
@RequiredArgsConstructor
public class CleanUserResponseHandler {

    private final CleanUserDispatcher dispatcher;
    private final DeviceRepository deviceRepository;
    private final DeviceUserAccessRepository deviceUserAccessRepository;
    private final UserRepository userRepository;

    @Transactional
    public void handleCleanUserResponse(JsonNode json, WebSocketSession session) {
        try {
            boolean result = json.path("result").asBoolean(false);
            String sn = (String) session.getAttributes().get("sn");

            if (!"cleanuser".equalsIgnoreCase(json.path("ret").asText(""))) return;
            if (sn == null) return;

            var pending = dispatcher.poll(sn);
            if (pending.isEmpty()) {
                System.err.println("⚠ ACK CLEANUSER recibido sin pending");
                return;
            }

            if (result) {
                System.out.println("Dispositivo confirmó CLEAN USER exitoso.");

                deviceRepository.findBySn(sn).ifPresent(device -> {
                    deviceUserAccessRepository.deleteByDeviceId(device.getId());
                    device.setPendingClean(false);
                    deviceRepository.save(device);
                    System.out.printf("🧹 AccessLinks eliminados para device=%s%n", sn);

                    registerAdminsPostClean(device);
                });
            } else {
                int reason = json.path("reason").asInt(-1);
                System.out.println("Falló CLEAN USER. Reason=" + reason);
            }

        } catch (Exception e) {
            System.err.println("❌ Error al procesar respuesta de cleanuser: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void registerAdminsPostClean(DeviceModel device) {
        Long companyId = device.getCompany().getId();

        // Buscar todos los admins de la empresa
        List<UserModel> admins = userRepository.findByCompanyIdAndAdminLevel(companyId, 1);

        for (UserModel admin : admins) {
            if (admin.getCredentials() == null || admin.getCredentials().isEmpty()) {
                System.out.printf("⚠ Admin %s no tiene credenciales válidas → NO se auto registra%n",
                        admin.getName());
                continue;
            }

            DeviceUserAccessModel newAccess = new DeviceUserAccessModel();
            newAccess.setDevice(device);
            newAccess.setUser(admin);
            newAccess.setEnrollId(admin.getEnrollId());
            newAccess.setEnabled(true);
            newAccess.setSynced(false);              // 🔥 se reenviará por scheduler
            newAccess.setPendingDelete(false);
            newAccess.setPendingNameSync(false);
            newAccess.setPendingStateSync(false);

            deviceUserAccessRepository.save(newAccess);
            System.out.printf("✅ Admin %s re-registrado para el device=%s pendiente de sync%n",
                    admin.getName(), device.getSn());
        }

        if (admins.isEmpty()) {
            System.out.println("ℹ️ No había admins con credenciales válidas para reinsertar.");
        }
    }
}
