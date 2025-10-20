package com.icm.biometric_zone_gate_api.dto;

import lombok.Data;

@Data
public class UserCredentialDTO {
    private String type;
    private Integer backupNum;
    private String record;
}
