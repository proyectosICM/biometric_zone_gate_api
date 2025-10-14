package com.icm.biometric_zone_gate_api.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetUserInfoRequest {
    private String cmd;
    private Integer enrollid;
    private Integer backupnum;
}
