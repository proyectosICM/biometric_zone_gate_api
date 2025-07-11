package com.icm.biometric_zone_gate_api.mappers;

import com.icm.biometric_zone_gate_api.dto.DeviceDTO;
import com.icm.biometric_zone_gate_api.models.DeviceModel;

public class DeviceMapper {

    public static DeviceDTO toDTO(DeviceModel model) {
        DeviceDTO dto = new DeviceDTO();
        dto.setId(model.getId());
        dto.setModel(model.getModel());
        dto.setFirmwareVersion(model.getFirmwareVersion());
        dto.setName(model.getName());
        dto.setActivo(model.getActivo());

        if (model.getSetting() != null) {
            dto.setSetting(DeviceSettingMapper.toDTO(model.getSetting()));
        }

        return dto;
    }

    public static DeviceModel toEntity(DeviceDTO dto) {
        DeviceModel model = new DeviceModel();
        model.setId(dto.getId());
        model.setModel(dto.getModel());
        model.setFirmwareVersion(dto.getFirmwareVersion());
        model.setName(dto.getName());
        model.setActivo(dto.getActivo());

        if (dto.getSetting() != null) {
            model.setSetting(DeviceSettingMapper.toEntity(dto.getSetting()));
        }

        return model;
    }
}

