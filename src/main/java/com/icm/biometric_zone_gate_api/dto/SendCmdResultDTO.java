package com.icm.biometric_zone_gate_api.dto;

public class SendCmdResultDTO extends DeviceAuthDTO{
    private String trans_id;
    private String cmd_return_code; // "OK" o "ERROR"
    // cuerpo específico depende del comando ejecutado
}
