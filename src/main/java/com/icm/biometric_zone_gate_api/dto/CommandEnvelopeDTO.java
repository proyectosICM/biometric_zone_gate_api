package com.icm.biometric_zone_gate_api.dto;

import lombok.Data;

// Un CommandEnvelopeDTO que represente cualquier comando enviado o recibido y tenga:
@Data
public class CommandEnvelopeDTO {
    private String codigoSolicitud;
    private String cmdCode;
    private String transId;
    private DeviceAuthDTO auth;
    private Object body; // luego haces casting según el cmdCode
}
