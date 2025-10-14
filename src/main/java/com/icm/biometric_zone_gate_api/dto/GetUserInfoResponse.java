package com.icm.biometric_zone_gate_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetUserInfoResponse {
    private String ret;
    private boolean result;
    private Integer enrollid;
    private String name;
    private Integer backupnum;
    private Integer admin;
    private Object record;
    private Integer reason; // opcional: solo si result = false
}
