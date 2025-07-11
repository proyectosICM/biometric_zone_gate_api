package com.icm.biometric_zone_gate_api.dto;

import lombok.Data;

// Evento de inscripción remota (realtime_enroll_data)
@Data
public class EnrollEventDTO extends UserDTO {
    private String tiempo;
}
