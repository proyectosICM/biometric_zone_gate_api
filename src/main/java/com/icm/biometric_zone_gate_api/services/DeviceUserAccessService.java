package com.icm.biometric_zone_gate_api.services;

import com.icm.biometric_zone_gate_api.dto.DeviceUserAccessDTO;
import com.icm.biometric_zone_gate_api.models.*;
import com.icm.biometric_zone_gate_api.repositories.DeviceRepository;
import com.icm.biometric_zone_gate_api.repositories.DeviceUserAccessRepository;
import com.icm.biometric_zone_gate_api.repositories.DeviceUserRepository;
import com.icm.biometric_zone_gate_api.repositories.UserRepository;
import com.icm.biometric_zone_gate_api.websocket.DeviceSessionManager;
import com.icm.biometric_zone_gate_api.websocket.commands.CleanUserCommandSender;
import com.icm.biometric_zone_gate_api.websocket.commands.DeleteUserCommandSender;
import com.icm.biometric_zone_gate_api.websocket.commands.EnableUserCommandSender;
import com.icm.biometric_zone_gate_api.websocket.commands.SetUserInfoCommandSender;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.icm.biometric_zone_gate_api.mappers.DeviceUserAccessMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceUserAccessService {
    private final DeviceUserAccessRepository deviceUserAccessRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final DeviceSessionManager sessionManager;
    private final SetUserInfoCommandSender commandSender;
    private final DeleteUserCommandSender deleteUserCommandSender;
    private final CleanUserCommandSender cleanUserCommandSender;
    private final DeviceUserRepository deviceUserRepository;
    private final EnableUserCommandSender enableUserCommandSender;

    public Optional<DeviceUserAccessModel> findById(Long id) {
        return deviceUserAccessRepository.findById(id);
    }

    public List<DeviceUserAccessDTO> findAll() {
        List<DeviceUserAccessModel> entities = deviceUserAccessRepository.findAll();
        return entities.stream()
                .map(DeviceUserAccessMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Page<DeviceUserAccessDTO> findAll(Pageable pageable) {
        Page<DeviceUserAccessModel> entities = deviceUserAccessRepository.findAll(pageable);

        // Mapear de Entity a DTO
        return new PageImpl<>(
                entities.stream()
                        .map(DeviceUserAccessMapper::toDTO)
                        .collect(Collectors.toList()),
                pageable,
                entities.getTotalElements()
        );
    }

    public DeviceUserAccessDTO save(DeviceUserAccessDTO dto) {
        DeviceUserAccessModel entity = DeviceUserAccessMapper.toEntity(dto);

        // --- Validar relaciones obligatorias --
        if (dto.getUserId() == null)
            throw new RuntimeException("El campo userId es obligatorio.");
        if (dto.getDeviceId() == null)
            throw new RuntimeException("El campo deviceId es obligatorio.");

        // Asociar entidades
        UserModel user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + dto.getUserId()));

        DeviceModel device = deviceRepository.findById(dto.getDeviceId())
                .orElseThrow(() -> new RuntimeException("Dispositivo no encontrado con ID: " + dto.getDeviceId()));

        entity.setUser(user);
        entity.setDevice(device);

        // Guardar en base de datos
        DeviceUserAccessModel saved = deviceUserAccessRepository.save(entity);

        // --- Intentar enviar al dispositivo ---
        var session = sessionManager.getSessionBySn(saved.getDevice().getSn());

        if (session != null && session.isOpen()) {
            try {
                if (user.getCredentials() == null || user.getCredentials().isEmpty()) {
                    System.err.println("Usuario sin credenciales, no se puede enviar al dispositivo.");
                } else {
                    for (UserCredentialModel credential : user.getCredentials()) {
                        int enrollId = user.getId().intValue();
                        String name = user.getName();
                        int backupNum = credential.getBackupNum();
                        int admin = user.getAdminLevel() != null ? user.getAdminLevel() : 0;
                        String record = credential.getRecord();

                        // 🔹 Imprimir TODOS los datos antes de enviar
                        System.out.println("===== Datos del usuario a enviar =====");
                        System.out.println("ID usuario: " + enrollId);
                        System.out.println("Nombre: " + name);
                        System.out.println("Admin level: " + admin);
                        System.out.println("BackupNum: " + backupNum);
                        System.out.println("Record: " + record);
                        System.out.println("Dispositivo: " + device.getSn());
                        System.out.println("=====================================");

                            /*
                            System.err.printf("Enviando usuario '%s' (ID=%d, backup=%d) al dispositivo %s%n",
                                    name, enrollId, backupNum, device.getSn());

                             */

                        commandSender.sendSetUserInfoCommand(session, enrollId, name, backupNum, admin, record);
                    }

                }
            } catch (Exception e) {
                System.err.println("❌ Error al enviar usuario al dispositivo: " + e.getMessage());
            }
        } else {
            System.out.printf(" Dispositivo %s no conectado. Usuario pendiente de descarga.%n",
                    saved.getDevice().getSn());
        }

        return DeviceUserAccessMapper.toDTO(saved);
    }

    public Optional<DeviceUserAccessModel> update(Long id, DeviceUserAccessModel updatedAccess) {
        return deviceUserAccessRepository.findById(id).map(existing -> {
            existing.setUser(updatedAccess.getUser());
            existing.setDevice(updatedAccess.getDevice());
            existing.setWeekZone(updatedAccess.getWeekZone());
            existing.setGroupNumber(updatedAccess.getGroupNumber());
            existing.setStartTime(updatedAccess.getStartTime());
            existing.setEndTime(updatedAccess.getEndTime());
            existing.setEnabled(updatedAccess.getEnabled());
            return deviceUserAccessRepository.save(existing);
        });
    }

    public boolean deleteById(Long id) {
        var accessOpt = deviceUserAccessRepository.findById(id);

        if (accessOpt.isEmpty()) {
            System.out.println("No existe DeviceUserAccess con ID: " + id);
            return false;
        }

        var access = accessOpt.get();

        // Obtener relaciones necesarias
        var user = access.getUser();
        var device = access.getDevice();

        if (user == null || device == null) {
            System.out.println("Usuario o dispositivo no definidos, abortando eliminación.");
            return false;
        }

        int enrollId = access.getEnrollId(); // 🔹 usamos el enrollId REAL del acceso
        if (enrollId <= 0) {
            System.out.println("Acceso sin enrollId válido, no se enviará DELETEUSER al dispositivo.");
        }

        // Buscar sesión activa del dispositivo
        var session = sessionManager.getSessionBySn(device.getSn());

        if (session != null && session.isOpen() && enrollId > 0) {
            try {
                if (!user.getCredentials().isEmpty()) {
                    for (var cred : user.getCredentials()) {
                        int backupNum = cred.getBackupNum();

                        // Enviar comando deleteuser para cada credencial
                        deleteUserCommandSender.sendDeleteUserCommand(session, enrollId, backupNum);

                        System.out.printf("🗑️ Enviado comando DELETEUSER (user=%d, backup=%d) al dispositivo %s%n",
                                enrollId, backupNum, device.getSn());

                        // Si quieres, aquí podrías agregar un pequeño delay con DeviceCommandScheduler
                        // deviceCommandScheduler.schedule(() -> deleteUserCommandSender.sendDeleteUserCommand(session, enrollId, backupNum), 200);
                    }
                } else {
                    System.out.println("Usuario sin credenciales, no se enviará DELETEUSER al dispositivo.");
                }
            } catch (Exception e) {
                System.err.println("Error al enviar deleteuser al dispositivo: " + e.getMessage());
            }
        } else {
            System.out.printf("Dispositivo %s no conectado. Eliminación pendiente.%n", device.getSn());
        }

        // Finalmente eliminar en base de datos
        deviceUserAccessRepository.deleteById(id);
        System.out.printf("Eliminado DeviceUserAccess con ID=%d de la base de datos.%n", id);

        return true;
    }

    public Optional<DeviceUserAccessDTO> findByUserIdAndDeviceId(Long userId, Long deviceId) {
        return deviceUserAccessRepository.findByUserIdAndDeviceId(userId, deviceId)
                .map(DeviceUserAccessMapper::toDTO);
    }

    public Optional<DeviceUserAccessDTO> findByUserIdAndDeviceIdAndEnabledTrue(Long userId, Long deviceId) {
        return deviceUserAccessRepository.findByUserIdAndDeviceIdAndEnabledTrue(userId, deviceId)
                .map(DeviceUserAccessMapper::toDTO);
    }

    public List<DeviceUserAccessDTO> findByUserId(Long userId) {
        List<DeviceUserAccessModel> entities = deviceUserAccessRepository.findByUserId(userId);
        return entities.stream()
                .map(DeviceUserAccessMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Page<DeviceUserAccessDTO> findByUserId(Long userId, Pageable pageable) {
        Page<DeviceUserAccessModel> page = deviceUserAccessRepository.findByUserId(userId, pageable);
        return page.map(DeviceUserAccessMapper::toDTO);
    }

    public List<DeviceUserAccessDTO> findByDeviceId(Long deviceId) {
        List<DeviceUserAccessModel> entities = deviceUserAccessRepository.findByDeviceId(deviceId);
        return entities.stream()
                .map(DeviceUserAccessMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Page<DeviceUserAccessDTO> findByDeviceId(Long deviceId, Pageable pageable) {
        Page<DeviceUserAccessModel> page = deviceUserAccessRepository.findByDeviceId(deviceId, pageable);
        return page.map(DeviceUserAccessMapper::toDTO);
    }

    public Page<DeviceUserAccessDTO> findByDeviceIdAndEnabledTrue(Long deviceId, Pageable pageable) {
        Page<DeviceUserAccessModel> page = deviceUserAccessRepository.findByDeviceIdAndEnabledTrue(deviceId, pageable);
        return page.map(DeviceUserAccessMapper::toDTO);
    }

    public List<DeviceUserAccessDTO> findByGroupNumber(Integer groupNumber) {
        List<DeviceUserAccessModel> entities = deviceUserAccessRepository.findByGroupNumber(groupNumber);
        return entities.stream()
                .map(DeviceUserAccessMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Page<DeviceUserAccessDTO> findByGroupNumber(Integer groupNumber, Pageable pageable) {
        Page<DeviceUserAccessModel> page = deviceUserAccessRepository.findByGroupNumber(groupNumber, pageable);
        return page.map(DeviceUserAccessMapper::toDTO);
    }

    public List<DeviceUserAccessDTO> findByEnabledTrue() {
        List<DeviceUserAccessModel> entities = deviceUserAccessRepository.findByEnabledTrue();
        return entities.stream()
                .map(DeviceUserAccessMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Page<DeviceUserAccessDTO> findByEnabledTrue(Pageable pageable) {
        Page<DeviceUserAccessModel> page = deviceUserAccessRepository.findByEnabledTrue(pageable);
        return page.map(DeviceUserAccessMapper::toDTO);
    }

    public void cleanDeviceUsersBySn(Long id) {
        // Buscar el dispositivo por ID
        DeviceModel device = deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dispositivo no encontrado con ID: " + id));

        String sn = device.getSn();

        // Obtener sesión activa
        var session = sessionManager.getSessionBySn(sn);

        if (session != null && session.isOpen()) {
            try {
                // Enviar comando CLEAN USER al dispositivo
                cleanUserCommandSender.sendCleanUserCommand(session);
                System.out.println("🧹 Comando CLEAN USER enviado al dispositivo " + sn);
            } catch (Exception e) {
                System.err.println("❌ Error al enviar CLEAN USER: " + e.getMessage());
            }
        } else {
            System.out.println("⚠️ Dispositivo " + sn + " no conectado. Comando pendiente.");
        }

        // Eliminar todos los DeviceUserAccess asociados a este dispositivo
        List<DeviceUserAccessModel> accesses = deviceUserAccessRepository.findByDeviceId(device.getId());
        if (!accesses.isEmpty()) {
            deviceUserAccessRepository.deleteAll(accesses);
            System.out.println("🗑️ Eliminados " + accesses.size() + " registros de DeviceUserAccess para dispositivo " + sn);
        } else {
            System.out.println("ℹ️ No hay registros de DeviceUserAccess para eliminar para dispositivo " + sn);
        }

        // Eliminar todos los DeviceUser asociados al dispositivo
        /*
        List<DeviceUserModel> deviceUsers = deviceUserRepository.findByDeviceId(device.getId());
        if (!deviceUsers.isEmpty()) {
            deviceUserRepository.deleteAll(deviceUsers);
            System.out.println("🗑️ Eliminados " + deviceUsers.size() + " registros de DeviceUser para dispositivo " + sn);
        } else {
            System.out.println("ℹ️ No hay registros de DeviceUser para eliminar para dispositivo " + sn);
        }
         */
    }

}
