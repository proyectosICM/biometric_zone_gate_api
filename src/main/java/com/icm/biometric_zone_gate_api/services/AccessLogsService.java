package com.icm.biometric_zone_gate_api.services;

import com.icm.biometric_zone_gate_api.enums.AccessType;
import com.icm.biometric_zone_gate_api.models.AccessLogsModel;
import com.icm.biometric_zone_gate_api.repositories.AccessLogsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccessLogsService {

    private final AccessLogsRepository accessLogsRepository;

    // Crear log de acceso
    public AccessLogsModel createLog(AccessLogsModel log) {
        return accessLogsRepository.save(log);
    }

    // Listar todos los logs
    public List<AccessLogsModel> getAllLogs() {
        return accessLogsRepository.findAll();
    }

    // Buscar log por ID
    public Optional<AccessLogsModel> getLogById(Long id) {
        return accessLogsRepository.findById(id);
    }

    // Buscar logs por usuario
    public List<AccessLogsModel> getLogsByUser(Long userId) {
        return accessLogsRepository.findByUserId(userId);
    }

    // Buscar logs por dispositivo
    public List<AccessLogsModel> getLogsByDevice(Long deviceId) {
        return accessLogsRepository.findByDeviceId(deviceId);
    }

    // Buscar logs por empresa
    public List<AccessLogsModel> getLogsByCompany(Long companyId) {
        return accessLogsRepository.findByCompanyId(companyId);
    }

    // Buscar logs por tipo de acción
    public List<AccessLogsModel> getLogsByAction(AccessType action) {
        return accessLogsRepository.findByAction(action);
    }

    // Eliminar log
    public boolean deleteLog(Long id) {
        if (accessLogsRepository.existsById(id)) {
            accessLogsRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
