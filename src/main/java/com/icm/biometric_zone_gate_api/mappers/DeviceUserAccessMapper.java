package com.icm.biometric_zone_gate_api.mappers;

import com.icm.biometric_zone_gate_api.dto.DeviceUserAccessDTO;
import com.icm.biometric_zone_gate_api.models.DeviceUserAccessModel;

public class DeviceUserAccessMapper {

    // De entity a DTO
    public static DeviceUserAccessDTO toDTO(DeviceUserAccessModel entity) {
        if (entity == null) return null;

        DeviceUserAccessDTO dto = new DeviceUserAccessDTO();
        dto.setId(entity.getId());
        dto.setEnabled(entity.getEnabled());
        dto.setEnrollId(entity.getEnrollId());
        dto.setSynced(entity.isSynced());
        dto.setPendingDelete(entity.isPendingDelete());

        if (entity.getUser() != null) {
            dto.setUserId(entity.getUser().getId());
            dto.setName(entity.getUser().getName());
            dto.setUserName(entity.getUser().getUsername());
        }

        if (entity.getDevice() != null) {
            dto.setDeviceId(entity.getDevice().getId());
            dto.setDeviceName(entity.getDevice().getName());
        }

        return dto;
    }

    // De DTO a entity (solo algunos campos básicos; no se asignan relaciones completas)
    public static DeviceUserAccessModel toEntity(DeviceUserAccessDTO dto) {
        if (dto == null) return null;

        DeviceUserAccessModel entity = new DeviceUserAccessModel();
        entity.setId(dto.getId());
        entity.setEnabled(dto.getEnabled());
        entity.setEnrollId(dto.getEnrollId());
        entity.setSynced(dto.isSynced());
        entity.setPendingDelete(dto.isPendingDelete());
        // user y device deben asignarse fuera del mapper, normalmente usando repositorios
        return entity;
    }
}
