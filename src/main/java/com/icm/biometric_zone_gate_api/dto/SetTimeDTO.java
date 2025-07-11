package com.icm.biometric_zone_gate_api.dto;


import lombok.Data;

// Sincronizar hora (SET_TIME)
@Data
public class SetTimeDTO {
    private String syncTime; // "aaaaMMddHHmmss"
}
