package com.icm.biometric_zone_gate_api.dto;

import lombok.Data;

import java.util.List;

// Obtener información de usuarios (GET_USER_INFO)
@Data
public class GetUserInfoRequestDTO {
    private List<String> usersId;
}

