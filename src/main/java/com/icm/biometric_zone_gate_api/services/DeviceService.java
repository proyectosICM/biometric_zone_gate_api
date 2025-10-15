package com.icm.biometric_zone_gate_api.services;

import com.icm.biometric_zone_gate_api.enums.DeviceStatus;
import com.icm.biometric_zone_gate_api.models.DeviceModel;
import com.icm.biometric_zone_gate_api.models.DeviceUserAccessModel;
import com.icm.biometric_zone_gate_api.models.DeviceUserModel;
import com.icm.biometric_zone_gate_api.models.UserModel;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.repositories.DeviceUserAccessRepository;
import com.icm.biometric_zone_gate_api.repositories.DeviceUserRepository;
import com.icm.biometric_zone_gate_api.websocket.DeviceSessionManager;
import com.icm.biometric_zone_gate_api.websocket.commands.EnableUserCommandSender;
import com.icm.biometric_zone_gate_api.websocket.commands.GetUserNameCommandSender;
import com.icm.biometric_zone_gate_api.websocket.commands.SetUserNameCommandSender;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceUserAccessRepository deviceUserAccessRepository;
    private final DeviceSessionManager deviceSessionManager;
    private final GetUserNameCommandSender getUserNameCommandSender;
    private final DeviceUserRepository deviceUserRepository;
    private final SetUserNameCommandSender setUserNameCommandSender;
    private final EnableUserCommandSender enableUserCommandSender;

    public DeviceModel createDevice(DeviceModel device) {
        return deviceRepository.save(device);
    }

    public List<DeviceModel> getAllDevices() {
        return deviceRepository.findAll();
    }

    public Page<DeviceModel> getAllDevices(Pageable pageable) {
        return deviceRepository.findAll(pageable);
    }

    public Optional<DeviceModel> getDeviceById(Long id) {
        return deviceRepository.findById(id);
    }

    public Optional<DeviceModel> getDeviceByName(String name) {
        return Optional.ofNullable(deviceRepository.findByName(name));
    }

    public Optional<DeviceModel> updateDevice(Long id, DeviceModel updatedDevice) {
        return deviceRepository.findById(id).map(device -> {
            device.setName(updatedDevice.getName());
            device.setSn(updatedDevice.getSn());
            device.setCompany(updatedDevice.getCompany());
            return deviceRepository.save(device);
        });
    }

    public boolean deleteDevice(Long id) {
        if (deviceRepository.existsById(id)) {
            deviceRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<DeviceModel> getDeviceBySn(String sn) {
        return deviceRepository.findBySn(sn);
    }

    public DeviceModel updateDeviceStatus(Long deviceId, DeviceStatus newStatus) {
        return deviceRepository.findById(deviceId).map(device -> {
            device.setStatus(newStatus);
            return deviceRepository.save(device);
        }).orElseThrow(() -> new RuntimeException("Device not found with id: " + deviceId));
    }

    public List<DeviceModel> listByCompany(Long companyId) {
        return deviceRepository.findByCompanyId(companyId);
    }

    public Page<DeviceModel> listByCompanyPaginated(Long companyId, Pageable pageable) {
        return deviceRepository.findByCompanyId(companyId, pageable);
    }

    public void requestUserName(Long deviceId, int enrollId) {
        Optional<DeviceModel> deviceOpt = deviceRepository.findById(deviceId);

        if (deviceOpt.isEmpty()) {
            throw new IllegalArgumentException("No existe el dispositivo con id " + deviceId);
        }

        DeviceModel device = deviceOpt.get();
        String sn = device.getSn();

        WebSocketSession session = deviceSessionManager.getSessionBySn(sn);
        if (session == null || !session.isOpen()) {
            throw new IllegalStateException("El dispositivo " + sn + " no está conectado.");
        }

        getUserNameCommandSender.sendGetUserNameCommand(session, enrollId);
    }

    public void broadcastUserNameUpdate(UserModel user) {
        try {
            List<DeviceUserModel> links = deviceUserRepository.findByUserId(user.getId());

            if (links.isEmpty()) {
                System.out.println("ℹ️ Usuario " + user.getUsername() + " no está asociado a ningún dispositivo.");
                return;
            }

            for (DeviceUserModel link : links) {
                DeviceModel device = link.getDevice();
                String sn = device.getSn();
                WebSocketSession session = deviceSessionManager.getSessionBySn(sn);

                if (session != null && session.isOpen()) {
                    System.out.println("📡 Enviando SET USERNAME al dispositivo SN=" + sn +
                            " (enrollId=" + link.getEnrollId() + ", name=" + user.getName() + ")");

                    var record = new SetUserNameCommandSender.UserRecord(link.getEnrollId(), user.getName());
                    setUserNameCommandSender.sendSetUserNameCommand(session, List.of(record));
                } else {
                    System.out.println("⚠️ Dispositivo SN=" + sn + " no conectado. No se puede actualizar nombre.");
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error al propagar cambio de nombre: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void broadcastUserEnableState(UserModel user) {
        try {
            List<DeviceUserModel> links = deviceUserRepository.findByUserId(user.getId());

            if (links.isEmpty()) {
                System.out.println("ℹ️ Usuario " + user.getUsername() + " no está asociado a ningún dispositivo.");
                return;
            }

            for (DeviceUserModel link : links) {
                DeviceModel device = link.getDevice();
                String sn = device.getSn();
                WebSocketSession session = deviceSessionManager.getSessionBySn(sn);

                if (session != null && session.isOpen()) {
                    boolean enabled = Boolean.TRUE.equals(user.getEnabled());

                    System.out.println("📡 Enviando ENABLE USER al dispositivo SN=" + sn +
                            " (enrollId=" + link.getEnrollId() + ", enabled=" + enabled + ")");

                    enableUserCommandSender.sendEnableUserCommand(session, link.getEnrollId(), enabled);

                } else {
                    System.out.println("⚠️ Dispositivo SN=" + sn + " no conectado. No se puede actualizar estado.");
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error al propagar cambio de estado ENABLE/DISABLE: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /*
    public void syncUsersFromDevice(Long deviceId) {
        DeviceWebSocketClient client = connectionManager.getClient(deviceId);
        if (client == null) {
            throw new IllegalStateException("Device not connected or not found.");
        }

        client.requestUserList();
    }

    public void pushUserToDevice(Long deviceId, UserModel user) {
        DeviceWebSocketClient client = connectionManager.getClient(deviceId);
        if (client == null) throw new IllegalStateException("Device not connected.");
        client.sendUserToDevice(user);
    }

    public void removeUserFromDevice(Long deviceId, Long userId) {
        DeviceWebSocketClient client = connectionManager.getClient(deviceId);
        if (client == null) throw new IllegalStateException("Device not connected.");
        client.deleteUserFromDevice(userId);
    }

    public void clearAllDeviceUsers(Long deviceId) {
        DeviceWebSocketClient client = connectionManager.getClient(deviceId);
        if (client == null) throw new IllegalStateException("Device not connected.");
        client.clearAllUsers();
    }

     */
}
