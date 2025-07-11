package com.icm.biometric_zone_gate_api.mappers;

import com.icm.biometric_zone_gate_api.dto.DeviceSettingDTO;
import com.icm.biometric_zone_gate_api.models.DeviceSettingModel;

public class DeviceSettingMapper {

    public static DeviceSettingDTO toDTO(DeviceSettingModel model) {
        DeviceSettingDTO dto = new DeviceSettingDTO();
        dto.setDeviceName(model.getDeviceName());
        dto.setServerHost(model.getServerHost());
        dto.setServerPort(model.getServerPort());
        dto.setIdioma(model.getIdioma());
        dto.setVolumen(model.getVolumen());
        dto.setAntiPass(model.getAntiPass());
        dto.setDoorOpenDelay(model.getDoorOpenDelay());
        dto.setVerificationMode(model.getVerificationMode());
        dto.setOldPwd(model.getOldPwd());
        dto.setNewPwd(model.getNewPwd());
        return dto;
    }

    public static DeviceSettingModel toEntity(DeviceSettingDTO dto) {
        DeviceSettingModel model = new DeviceSettingModel();
        model.setDeviceName(dto.getDeviceName());
        model.setServerHost(dto.getServerHost());
        model.setServerPort(dto.getServerPort());
        model.setIdioma(dto.getIdioma());
        model.setVolumen(dto.getVolumen());
        model.setAntiPass(dto.getAntiPass());
        model.setDoorOpenDelay(dto.getDoorOpenDelay());
        model.setVerificationMode(dto.getVerificationMode());
        model.setOldPwd(dto.getOldPwd());
        model.setNewPwd(dto.getNewPwd());
        return model;
    }
}
