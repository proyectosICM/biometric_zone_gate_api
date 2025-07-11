package com.icm.biometric_zone_gate_api.dto;

import lombok.Data;

import java.util.List;

@Data
public class GetUserInfoRequestDTO {
    private List<String> usersId;
}

