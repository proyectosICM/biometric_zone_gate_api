package com.icm.biometric_zone_gate_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceInfoDTO {
    private String cmd = "setdevinfo";
    private int deviceid;
    private int language;
    private int volume;
    private boolean sleep;
    private int verificationMode;
    private int userFpNum;
    private int logHint;
    private int reverifyTime;
    private int antiPassback;
}
