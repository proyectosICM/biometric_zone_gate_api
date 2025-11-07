package com.icm.biometric_zone_gate_api.dto;

import lombok.Data;

@Data
public class AccessLogEppPatchDTO {
    private Boolean correctEpp;
    private String entryEppPhotoB64;
}
