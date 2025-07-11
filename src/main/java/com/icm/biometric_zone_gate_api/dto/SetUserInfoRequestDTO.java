package com.icm.biometric_zone_gate_api.dto;

import lombok.Data;

import java.util.List;

// Sincronización masiva de usuarios
@Data
public class SetUserInfoRequestDTO extends DeviceAuthDTO {
    private List<UserDTO> usuarios;
}

