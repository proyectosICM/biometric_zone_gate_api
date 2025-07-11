package com.icm.biometric_zone_gate_api.dto;

import lombok.Data;

@Data
public class DeviceSettingDTO {
    private String deviceName;
    private String serverHost;
    private Integer serverPort;
    private String idioma;
    private Integer volumen;
    private Boolean antiPass;
    private Integer doorOpenDelay;
    private String verificationMode;
    private String oldPwd; // Puede omitirse si no guardas claves
    private String newPwd; // Puede omitirse si no guardas claves
    // Agrega los que necesites según el protocolo
}
